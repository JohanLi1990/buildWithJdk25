package disruptor.practice.oms.model;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public class FamilyState {
    AtomicInteger busy;
    Queue<TaskObject> pending;

    public FamilyState(AtomicInteger busy, Queue<TaskObject> q) {
        this.busy = busy;
        this.pending = q;
    }

    public AtomicInteger getBusy() {
        return busy;
    }

    public Queue<TaskObject> getPending() {
        return pending;
    }
}