package disruptor.practice.simple;

import disruptor.practice.common.IEvent;

public class LongEvent implements IEvent {

    private long id;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
