package disruptor.practice.oms.handlers;

import com.lmax.disruptor.EventHandler;
import disruptor.practice.common.CompletionSink;
import disruptor.practice.common.ReleaseHook;
import disruptor.practice.oms.model.ConfigHolder;
import disruptor.practice.oms.model.Decision;
import disruptor.practice.oms.model.FamilyState;
import disruptor.practice.oms.model.StageCtx;
import disruptor.practice.oms.model.TaskEvent;
import disruptor.practice.oms.model.TaskObject;
import disruptor.practice.oms.model.TaskResponse;
import disruptor.practice.oms.model.TaskType;
import disruptor.practice.oms.monitoring.HdrLatencyRecorder;
import disruptor.practice.oms.monitoring.LatencyRecorder;
import disruptor.practice.oms.monitoring.PartitionMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.Lock;
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
    private final CompletionSink sink;
    private final ReleaseHook releaseHook;
    private final AtomicInteger tick = new AtomicInteger();
    private static final ThreadLocal<byte[]> TL_BUF = ThreadLocal.withInitial(() -> new byte[256 * 1024]);
    static volatile int ALLOC_SINK; // basically tells JIT, do NOT optimize the allocation there, do not
    // reorder, or eliminate (visiblie and memory barriers)

    public int tick() {
        return tick.incrementAndGet();
    }

    public PartitionMetrics getMetrics() {
        return metrics;
    }

    private final PartitionMetrics metrics;
//    private final LatencyRecorder hotRecorder;
//    private final LatencyRecorder coldRecorder;
    private final HdrLatencyRecorder hot = new HdrLatencyRecorder();
    private final HdrLatencyRecorder cold = new HdrLatencyRecorder();
    private final ConfigHolder config;

    /**
     * Dispatcher thread, submit task to ThreadPoolExecutor for asynchronous execution
     *
     * @param partitionId
     * @param es
     */

    public DisruptorBusinessHandler(int partitionId, ThreadPoolExecutor es, CompletionSink sink,
                                    ReleaseHook releaseHook, ConfigHolder config) {
        this.partitionId = partitionId;
        this.tpe = es;
        this.metrics = new PartitionMetrics(new AtomicLong(0), new LongAdder(), new LongAdder(), new AtomicLong(0),
                new AtomicLong(0), new AtomicLong(0), new AtomicInteger(0));
        this.sink = sink;
        this.releaseHook = releaseHook;
//        this.hot = new HdrLatencyRecorder();
//        this.cold = new HdrLatencyRecorder();
        this.config = config;
    }


    @Override
    public void onEvent(TaskEvent event, long sequence, boolean endOfBatch) throws Exception {
//        tasksIn++;
        metrics.onTaskIn();
        TaskObject cur = new TaskObject(event, endOfBatch);
        int familyKey = event.getCorrelationId();
        familyKey = familyKey < 0 ? (int) event.getOrderId() % 18 : familyKey;
        FamilyState state = map.computeIfAbsent(familyKey, (_) -> new FamilyState(new AtomicInteger(0),
                new LinkedBlockingQueue<>()));
        cur.setT1Enq(System.nanoTime());
        if (state.getBusy().compareAndSet(0, 1)) {
            metrics.onDispatched();
            metrics.onAddInFlightFamiliesCount();
            dispatchAndDrain(state, cur);
        } else {
            // Drop the TaskEvent if current family queue is over 1024 or whatever the pendingLimit is
            if (state.getPendingCount() >= config.pendingLimit()) {
                TaskResponse rejectOverLimitResponse = new TaskResponse(cur.getOrderId(), cur.getCorrelationId(),
                        partitionId, cur.getSeqInFamily(), "REJ_OVER_LIMIT", System.nanoTime(), System.nanoTime(),
                        cur.getPayload());
                // still use the sink to send message back
                sink.onComplete(cur, rejectOverLimitResponse);
                metrics.onRejectedOverLimit();
            } else {
                state.enqueue(cur);
                metrics.onEnqueuedDueToBusy();
                metrics.onMaxPendingDepthObserved(state.getPendingCount());
            }
        }
    }

    private void dispatchAndDrain(FamilyState state, TaskObject event) {
        // before session 5
//        singleStageProcessing(state, event);
        multiStageProcessing(state, event);
    }

    private void singleStageProcessing(FamilyState state, TaskObject event) {
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
            TaskResponse taskResponse = new TaskResponse(event.getOrderId(), event.getCorrelationId(), partitionId,
                    event.getSeqInFamily(), s, event.getT0(), System.nanoTime(), event.getPayload());
            sink.onComplete(event, taskResponse);
            metrics.onCompleted();
            if ((metrics.currentCompleted() & 255) == 0) {
                metrics.debugLog(partitionId, tpe.getPoolSize(), tpe.getActiveCount(), tpe.getQueue().size());
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
        long tSubmit = System.nanoTime();
        CompletableFuture.supplyAsync(() -> {
            long t2Start = System.nanoTime();
            StageCtx ctx = new StageCtx();
            ctx.setT0(taskObject.getT0());
            validationAndBusinessParsing(taskObject, ctx);
            riskChecks(taskObject, ctx);
            execSimulation(taskObject, ctx);
            buildResponse(taskObject, ctx);

            long qTotal = t2Start - taskObject.getT1Enq(); // “time since arrival to work start”
            long execQ = t2Start - tSubmit;                // “executor wait” (optional but useful)
            long e2e = (ctx.getT4() - ctx.getT0());
            long svc = ctx.getT4() - t2Start;               // “work time”

            if (taskObject.getCorrelationId() == 11) {
                // hot
                hot.recordE2e(e2e);
                hot.recordSvc(svc);
                hot.recordQ(qTotal);
                hot.recordExecQ(execQ);
            } else {
                // hot
                cold.recordE2e(e2e);
                cold.recordSvc(svc);
                cold.recordQ(qTotal);
                cold.recordExecQ(execQ);
            }


            return ctx;
        }, tpe).thenAccept((ctx -> {
            sink.onComplete(taskObject, ctx.getTaskResponse());
            metrics.onCompleted();
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
//        LockSupport.parkNanos(15_000_000);// park 15ms
        LockSupport.parkNanos(1_000_000); // park 1ms
        if (config.ALLOC_HOTSPOT()) {
            byte[] fake = config.ALLOC_MITIGATION() ? TL_BUF.get() : new byte[256 * 1024];
            ALLOC_SINK ^= fake[23];
        }
        ctx.setExecResult("ACK");
        ctx.setT3(System.nanoTime());
    }

    /**
     * Stage 4: Build exactly one response
     *
     * @param t
     * @param ctx
     * @return
     */
    private void buildResponse(TaskObject t, StageCtx ctx) {
        if (Decision.REJECT == ctx.getDecision()) {
            ctx.setTaskResponse(new TaskResponse(t.getOrderId(), t.getCorrelationId(), partitionId, t.getSeqInFamily()
                    , ctx.getDecision().toString(), ctx.getT0(), System.nanoTime(), t.getPayload()));
        } else {
            ctx.setTaskResponse(new TaskResponse(t.getOrderId(), t.getCorrelationId(), partitionId,
                    t.getSeqInFamily(), ctx.getDecision().toString(), ctx.getT0(), System.nanoTime(), t.getPayload()));
        }
        ctx.setT4(System.nanoTime());
    }

    public record PendingStats(long familiesSeen, long familiesWithPending, long pendingTotal, long pendingMaxNow) {
    }

    public PendingStats samplePending() {
        long famSeen = this.map.size();
        long familiesWithPending = 0;
        long pendingTotal = 0;
        long pendingMax = 0;
        for (var ent : map.entrySet()) {
            int pendingSize = ent.getValue().getPendingCount();
            if (pendingSize > 0) {
                familiesWithPending++;
                pendingTotal += pendingSize;
                pendingMax = Math.max(pendingSize, pendingMax);
            }
        }
        return new PendingStats(famSeen, familiesWithPending, pendingTotal, pendingMax);
    }


    private void releaseLoop(FamilyState state, TaskObject event) {
        TaskObject next = state.dequeue();
        if (next != null) {
            metrics.onDispatched();
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

        next = state.dequeue();
        // this windows is actually safe because busy flag is already rest to 0
        // If busy==0, producers are responsible for (re)starting the drain.
        if (next == null) {
            metrics.onDecInFlightFamiliesCount();
            return;
        }
        // reclaim and continue draining
        if (state.getBusy().compareAndSet(0, 1)) {
            metrics.onDispatched();
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

    public PartitionMetrics.PartitionMetricSnapshot metricSnapshot(long nowNs) {
        return this.metrics.snapshot(nowNs, tpe.getPoolSize(), tpe.getActiveCount(), tpe.getQueue().size());
    }

    public int[] tpeState() {
        return new int[]{tpe.getPoolSize(), tpe.getActiveCount(), tpe.getQueue().size()};
    }


    public PartitionMetrics metrics() {
        return this.metrics;
    }

    public HdrLatencyRecorder.LatencySnapshot hotLatencySnapshot() {
        return this.hot.snapshotInterval();
    }

    public HdrLatencyRecorder.LatencySnapshot coldLatencySnapshot() {
        return this.cold.snapshotInterval();
    }
}
