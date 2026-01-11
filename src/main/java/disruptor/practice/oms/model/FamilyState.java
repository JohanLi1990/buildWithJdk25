package disruptor.practice.oms.model;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public class FamilyState {
    AtomicInteger busy;
    Queue<TaskObject> pending;
    AtomicInteger pendingCount;

    public FamilyState(AtomicInteger busy, Queue<TaskObject> q) {
        this.busy = busy;
        this.pending = q;
        this.pendingCount = new AtomicInteger(0);
    }

    public AtomicInteger getBusy() {
        return busy;
    }

    public Queue<TaskObject> getPending() {
        return pending;
    }

    public void enqueue(TaskObject taskObject) {
        this.pending.offer(taskObject);
        this.pendingCount.incrementAndGet();
    }

    public TaskObject dequeue() {
        this.pendingCount.decrementAndGet();
        return pending.poll();
    }

    public int getPendingCount() {
        return this.pendingCount.get();
    }
}