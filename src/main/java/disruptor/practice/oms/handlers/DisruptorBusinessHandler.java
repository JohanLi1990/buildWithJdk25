package disruptor.practice.oms.handlers;

import com.lmax.disruptor.EventHandler;
import disruptor.practice.oms.model.TaskEvent;
import disruptor.practice.oms.model.TaskResponse;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


public class DisruptorBusinessHandler implements EventHandler<TaskEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DisruptorBusinessHandler.class);
    private final static String ROUTED = "ROUTED";
    private final static String PARKED = "PARKED";
    private final static String TARGET = "TARGET";
    // correlationId used directly as familyKey,
    private final ConcurrentHashMap<Integer, FamilyState> map = new ConcurrentHashMap<>();
    private final int partitionId;
    private final ThreadPoolExecutor tpe;
    private long tasksIn;
    private final AtomicLong tasksCompleted;
    private final AtomicLong tasksDispatched;
    private long enqueuedDueToBusy;
    private long maxPendingDepthObserved;

    /**
     * Dispatcher thread, submit task to ThreadPoolExecutor for asynchronous execution
     *
     * @param partitionId
     * @param es
     */

    public DisruptorBusinessHandler(int partitionId, ThreadPoolExecutor es) {
        this.partitionId = partitionId;
        this.tpe = es;
        tasksIn = 0;
        tasksCompleted = new AtomicLong();
        tasksDispatched = new AtomicLong();
        enqueuedDueToBusy = 0;
        maxPendingDepthObserved = 0;
    }


    @Override
    public void onEvent(TaskEvent event, long sequence, boolean endOfBatch) throws Exception {
        tasksIn++;
        TaskObject cur = new TaskObject(event, endOfBatch);
        FamilyState state = map.computeIfAbsent(event.getCorrelationId(), (_) -> new FamilyState(new AtomicInteger(0),
                new LinkedBlockingQueue<>()));
        if (state.busy.compareAndSet(0, 1)) {
            tasksDispatched.incrementAndGet();
            dispatchAndDrain(state, cur);
        } else {
            state.pending.offer(cur);
            enqueuedDueToBusy++;
            maxPendingDepthObserved = Math.max(state.pending.size(), maxPendingDepthObserved);
        }
    }

    private void dispatchAndDrain(FamilyState state, TaskObject event) {
        CompletableFuture.supplyAsync(() -> {
            try {
                LOGGER.debug("partition-{} processing heavy logic for event-id {}, family:{}", partitionId,
                        event.getOrderId(),
                        event.getCorrelationId());
                Thread.sleep(20);
                return simpleProcess(event);
            } catch (Exception e) {
                LOGGER.info("Thread interrupted");
                return "Event not processed";
            }
        }, tpe).thenAccept((s) -> {
            writeResponseBackToNetty(event, s);
            long curCompleted = tasksCompleted.incrementAndGet();
            if ((curCompleted & 255) == 0) {
                LOGGER.info("partition {} -> tasksIn:{}; tasksCompleted:{}; tasksDispatched:{}; enqueuedDueToBusy:{}; " +
                                "maxPendingDepthObserved:{}, currentPoolSize:{}, active:{}, execQueue:{}",
                        partitionId, tasksIn, curCompleted, tasksDispatched.get(), enqueuedDueToBusy,
                        maxPendingDepthObserved, tpe.getPoolSize(), tpe.getActiveCount(), tpe.getQueue().size());
            }
        }).whenComplete((_, ex) -> {
            if (ex != null) {
                LOGGER.error("partition {} family {} failed", partitionId, event.getCorrelationId());
            }

            TaskObject next = state.pending.poll();
            if (next != null) {
                tasksDispatched.incrementAndGet();
                dispatchAndDrain(state, next);
            } else {
                // the next 4 lines of code is the race-fix, Lost Wakeup Prevention pattern
                state.busy.set(0);
                // prevent a task being stranded, when the task came in just between next = state.pending.poll() and
                // state.busy.set(0)
                if ((next = state.pending.poll()) != null && state.busy.compareAndSet(0, 1)) {
                    tasksDispatched.incrementAndGet();
                    dispatchAndDrain(state, next);
                }
            }
        });
    }

    private void writeResponseBackToNetty(TaskObject taskObject, String res) {
        // asynchronously write back
        taskObject.getChannel().write(new TaskResponse( taskObject.getOrderId(), taskObject.getCorrelationId(),
                partitionId, res, System.nanoTime()));
        if (taskObject.isEob()) {
            taskObject.getChannel().flush();
        }
    }

    /**
     * Used in Day5 session 1 & 2
     *
     * @param event
     * @return
     */
    private String simpleProcess(TaskObject event) {
        return switch ((int) (event.getOrderId() % 3)) {
            case 1 -> ROUTED;
            case 2 -> PARKED;
            default -> TARGET;
        };
    }

    static class FamilyState {
        AtomicInteger busy;
        Queue<TaskObject> pending;

        public FamilyState(AtomicInteger busy, Queue<TaskObject> q) {
            this.busy = busy;
            this.pending = q;
        }
    }

    static class TaskObject{

        private final long orderId;
        private final int correlationId;
        private final Channel channel;
        private final boolean isEob;
        private final String payload;

        TaskObject(TaskEvent te, boolean isEob) {
            this.orderId = te.getOrderId();
            this.correlationId = te.getCorrelationId();
            this.channel = te.getChannel();
            this.isEob = isEob;
            this.payload = te.getPayload();
        }

        public long getOrderId() {
            return orderId;
        }

        public int getCorrelationId() {
            return correlationId;
        }

        public Channel getChannel() {
            return channel;
        }

        public boolean isEob() {
            return isEob;
        }

        public String getPayload() {
            return payload;
        }
    }
}
