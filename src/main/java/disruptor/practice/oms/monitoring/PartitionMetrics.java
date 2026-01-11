package disruptor.practice.oms.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public record PartitionMetrics(AtomicLong tasksIn, LongAdder completed, LongAdder dispatched,
                               AtomicLong enqueuedDueToBusy, AtomicLong maxPendingDepthObserved,
                               AtomicLong inFlightFamiliesCount, AtomicInteger rejOverLimit) {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartitionMetrics.class);

    public void onRejectedOverLimit() {
        this.rejOverLimit.incrementAndGet();

    }
    public void onTaskIn() {
        this.tasksIn.incrementAndGet();
    }

    public void onCompleted() {
        this.completed.increment();
    }

    public void onDispatched() {
        this.dispatched.increment();
    }

    public void onEnqueuedDueToBusy() {
        this.enqueuedDueToBusy.incrementAndGet();
    }

    public void onMaxPendingDepthObserved(long newSize) {
        // race condition here... but it is okay, we are getting a rough estimates only
        long cur = maxPendingDepthObserved.get();
        if (newSize > cur) {
            maxPendingDepthObserved.compareAndSet(cur, newSize);
        }
    }

    public void onAddInFlightFamiliesCount(){
        this.inFlightFamiliesCount.incrementAndGet();
    }

    public void onDecInFlightFamiliesCount() {
        this.inFlightFamiliesCount.decrementAndGet();
    }

    public long currentCompleted() {
        return this.completed.sum();
    }

    public void debugLog(int partitionId, int poolSize, int active, int qSize) {
        long curCompleted = this.completed.sum();
        long curTasksIn = this.tasksIn.get();
        long curDispatched = this.dispatched.sum();
        long curEnqueued = this.enqueuedDueToBusy.get();
        long curMaxPending = this.maxPendingDepthObserved.get();

        LOGGER.debug("partition {} -> tasksIn:{}; tasksCompleted:{}; tasksDispatched:{}; " +
                        "enqueuedDueToBusy:{}; " +
                        "maxPendingDepthObserved:{}, currentPoolSize:{}, active:{}, execQueue:{}",
                partitionId, curTasksIn, curCompleted, curDispatched, curEnqueued,
                curMaxPending, poolSize, active, qSize);
    }

    public record PartitionMetricSnapshot(long taskIn, long completed, long dispatched, long enqueuedDueToBusy
            , long maxObserved, long inFlightFamilies, long nanoNs, int poolSize, int activeThreads, int qSize){}

    public PartitionMetricSnapshot snapshot(long nano, int poolSize, int active, int qSize) {
        long curCompleted = this.completed.sum();
        long curTasksIn = this.tasksIn.get();
        long curDispatched = this.dispatched.sum();
        long curEnqueued = this.enqueuedDueToBusy.get();
        long curMaxPending = this.maxPendingDepthObserved.get();
        long inFlightFamilies = this.inFlightFamiliesCount.get();
        return new PartitionMetricSnapshot(curTasksIn, curCompleted, curDispatched, curEnqueued, curMaxPending,
                inFlightFamilies, nano, poolSize, active, qSize);
    }



}
