package disruptor.practice.oms.handlers;

import com.lmax.disruptor.EventHandler;
import disruptor.practice.common.CompletionSink;
import disruptor.practice.common.ReleaseHook;
import disruptor.practice.oms.model.FamilyState;
import disruptor.practice.oms.model.TaskEvent;
import disruptor.practice.oms.model.TaskObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;


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
    private final CompletionSink sink;
    private final ReleaseHook releaseHook;

    /**
     * Dispatcher thread, submit task to ThreadPoolExecutor for asynchronous execution
     *
     * @param partitionId
     * @param es
     */

    public DisruptorBusinessHandler(int partitionId, ThreadPoolExecutor es, CompletionSink sink, ReleaseHook releaseHook) {
        this.partitionId = partitionId;
        this.tpe = es;
        tasksIn = 0;
        tasksCompleted = new AtomicLong();
        tasksDispatched = new AtomicLong();
        enqueuedDueToBusy = 0;
        maxPendingDepthObserved = 0;
        this.sink = sink;
        this.releaseHook = releaseHook;
    }


    @Override
    public void onEvent(TaskEvent event, long sequence, boolean endOfBatch) throws Exception {
        tasksIn++;
        TaskObject cur = new TaskObject(event, endOfBatch);
        FamilyState state = map.computeIfAbsent(event.getCorrelationId(), (_) -> new FamilyState(new AtomicInteger(0),
                new LinkedBlockingQueue<>()));
        if (state.getBusy().compareAndSet(0, 1)) {
            tasksDispatched.incrementAndGet();
            dispatchAndDrain(state, cur);
        } else {
            state.getPending().offer(cur);
            enqueuedDueToBusy++;
            maxPendingDepthObserved = Math.max(state.getPending().size(), maxPendingDepthObserved);
        }
    }

    private void dispatchAndDrain(FamilyState state, TaskObject event) {
        CompletableFuture.supplyAsync(() -> {
            try {
                LOGGER.debug("partition-{} processing heavy logic for event-id {}, family:{}", partitionId,
                        event.getOrderId(),
                        event.getCorrelationId());
                LockSupport.parkNanos(10_000_000L);
                return simpleProcess(event);
            } catch (Exception e) {
                LOGGER.info("Thread interrupted");
                return "Event not processed";
            }
        }, tpe).thenAccept((s) -> {
            sink.onComplete(event, s, partitionId);
            long curCompleted = tasksCompleted.incrementAndGet();
            if ((curCompleted & 255) == 0) {
                LOGGER.debug("partition {} -> tasksIn:{}; tasksCompleted:{}; tasksDispatched:{}; " +
                                "enqueuedDueToBusy:{}; " +
                                "maxPendingDepthObserved:{}, currentPoolSize:{}, active:{}, execQueue:{}",
                        partitionId, tasksIn, curCompleted, tasksDispatched.get(), enqueuedDueToBusy,
                        maxPendingDepthObserved, tpe.getPoolSize(), tpe.getActiveCount(), tpe.getQueue().size());
            }
        }).whenComplete((_, ex) -> {
            if (ex != null) {
                LOGGER.error("partition {} family {} failed, {}", partitionId, event.getCorrelationId(), ex.getMessage());
            }

            // cannonical, release + recheck + reclaim
            releaseLoop(state, event);
        });
    }

    private void releaseLoop(FamilyState state, TaskObject event) {
        TaskObject next = state.getPending().poll();
        if (next != null) {
            tasksDispatched.incrementAndGet();
            dispatchAndDrain(state, next);
            return;
        }
        // testHook extensions
        // this window is the real dangerous windows, that is why we have the "recheck" logic from 119 to 128
        releaseHook.onAboutToRelease(event.getCorrelationId(), state);
        if (!state.getBusy().compareAndSet(1, 0)) {
            LOGGER.warn("family {} busy CAS (1 -> 0) failed; busy={}", event.getCorrelationId(), state.getBusy().get());
            return;
        }

        next = state.getPending().poll();
        // this windows is actually safe because busy flag is already rest to 0
        // If busy==0, producers are responsible for (re)starting the drain.
        if (next == null) {
            return;
        }
        // reclaim and continue draining
        if (state.getBusy().compareAndSet(0, 1)) {
            tasksDispatched.incrementAndGet();
            dispatchAndDrain(state, next);
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

    @Override
    public void onShutdown() {
        tpe.shutdownNow();
        try{
            if (!this.tpe.awaitTermination(1, TimeUnit.SECONDS)) {
                this.tpe.shutdownNow();
            }
        } catch (InterruptedException e) {
            LOGGER.error("Cannot shut down ThreadPoolExecutor");
            Thread.currentThread().interrupt();
            tpe.shutdownNow();

        }


    }
}
