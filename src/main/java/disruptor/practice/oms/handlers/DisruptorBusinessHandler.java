package disruptor.practice.oms.handlers;

import com.lmax.disruptor.EventHandler;
import disruptor.practice.common.CompletionSink;
import disruptor.practice.common.ReleaseHook;
import disruptor.practice.oms.model.Decision;
import disruptor.practice.oms.model.FamilyState;
import disruptor.practice.oms.model.StageCtx;
import disruptor.practice.oms.model.TaskEvent;
import disruptor.practice.oms.model.TaskObject;
import disruptor.practice.oms.model.TaskResponse;
import disruptor.practice.oms.model.TaskType;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
        disruptor.practice.oms.model.TaskObject cur = new disruptor.practice.oms.model.TaskObject(event, endOfBatch);
        disruptor.practice.oms.model.FamilyState state = map.computeIfAbsent(event.getCorrelationId(), (_) -> new disruptor.practice.oms.model.FamilyState(new AtomicInteger(0),
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

    private void dispatchAndDrain(disruptor.practice.oms.model.FamilyState state, disruptor.practice.oms.model.TaskObject event) {
        // before session 5
//        singleStageProcessing(state, event);
        multiStageProcessing(state, event);
    }

    private void singleStageProcessing(disruptor.practice.oms.model.FamilyState state, disruptor.practice.oms.model.TaskObject event) {
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

    /**
     * Multi-stage processing, stage 1 to 4
     */

    private void multiStageProcessing(FamilyState state, TaskObject taskObject) {
        CompletableFuture.supplyAsync(() -> {
            StageCtx ctx = new StageCtx();
            ctx.setT0(taskObject.getT0());
            validationAndBusinessParsing(taskObject, ctx);
            riskChecks(taskObject, ctx);
            execSimulation(taskObject, ctx);
            buildResponse(taskObject, ctx);

            long e2e = (ctx.getT4() - ctx.getT0()); // in ns
            long s1 = (ctx.getT1() - ctx.getT0());
            long s2 = (ctx.getT2() - ctx.getT1());
            long s3 = (ctx.getT3() - ctx.getT2());
            long s4 = (ctx.getT4() - ctx.getT3());
            LOGGER.debug("OrderId:{}, correlationId:{}, seq:{}, decision:{} , e2e:{}ns, s1:{}ns, s2:{}ns, s3:{}ns, " +
                            "s4:{}ns ",
                    taskObject.getOrderId(), taskObject.getCorrelationId(), taskObject.getSeqInFamily(),
                    ctx.getDecision(), e2e, s1, s2, s3, s4);
            return ctx;
        }, tpe).thenAccept((ctx -> {
            // no completion sink involved here.. straight away async write back
            Channel channel = taskObject.getChannel();
            channel.eventLoop().execute(() -> {
                channel.write(ctx.getTaskResponse());
                if (taskObject.isEob()) {
                    channel.flush();
                }
            });
        })).whenComplete((_, ex) -> {
            if (ex != null) {
                LOGGER.error("partition {} family {} failed, {}", partitionId, taskObject.getCorrelationId(), ex.getMessage());
            }

            // cannonical, release + recheck + reclaim
            releaseLoop(state, taskObject);
        });
    }

    /**
     * Stage One: semantic validation + business parse
     */
    private void validationAndBusinessParsing(TaskObject t,
                                              StageCtx ctx) {
        // 0) decoder-classified invalid message shortcut
        if (TaskType.INVALID == t.getTaskType()) {
            ctx.setDecision(Decision.REJECT);
            ctx.setRejCode("BAD_MSG");
            ctx.setT1(System.nanoTime());
            return;
        }

        // 1) basic semantic checks
        if (t.getPayload() == null || t.getPayload().isEmpty()) {
            ctx.setDecision(Decision.REJECT);
            ctx.setRejCode("BAD_MSG:EMPTY_PAYLOAD");
            ctx.setT1(System.nanoTime());
            return;
        }

        // 2) omit parsing for now:
        ctx.setParsed(t.getPayload());
        ctx.setDecision(Decision.ACCEPT);
        ctx.setT1(System.nanoTime());

    }

    /**
     * Stage 2: risk checks
     *
     * @param t
     * @param ctx
     */
    private void riskChecks(TaskObject t, StageCtx ctx) {
        if (Decision.REJECT == ctx.getDecision()) {
            ctx.setT2(System.nanoTime());
            return;
        }

        // simulate risk checks
        if (ctx.getParsed().contains("BITCOIN")) {
            ctx.setDecision(Decision.REJECT);
            ctx.setRejCode("RISK_BANNED_SYMBOL");
            ctx.setT2(System.nanoTime());
            return;
        }

        ctx.setT2(System.nanoTime());
    }

    /**
     * Stage 3 - exeution simulation
     *
     * @param t
     * @param ctx
     */
    private void execSimulation(TaskObject t, StageCtx ctx) {
        if (ctx.getDecision() == Decision.REJECT) {
            ctx.setT3(System.nanoTime());
            return;
        }
        // mock downstream cost (10 - 20 ms)
        // don't sleep in real p99 benchmarking, but ok for mock
        LockSupport.parkNanos(15_000_000); // park 15ms
        ctx.setExecResult("ACK");
        ctx.setT3(System.nanoTime());
    }

    /**
     * Stage 4: Build exactly one response
     * @param t
     * @param ctx
     * @return
     */
    private void buildResponse(TaskObject t, StageCtx ctx) {
        if (Decision.REJECT == ctx.getDecision()) {
            ctx.setTaskResponse(new TaskResponse(t.getOrderId(), t.getCorrelationId(),partitionId, t.getSeqInFamily()
                    , ctx.getDecision().toString(), ctx.getT0(), System.nanoTime(), t.getPayload()));
        } else {
            ctx.setTaskResponse(new TaskResponse(t.getOrderId(), t.getCorrelationId(), partitionId,
                    t.getSeqInFamily(), ctx.getDecision().toString(), ctx.getT0(), System.nanoTime(), t.getPayload()));
        }
        ctx.setT4(System.nanoTime());
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
        try {
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
