package disruptor.practice.multiconsumer;

import disruptor.practice.common.IEvent;

public class MultiConsumerEvent implements IEvent {


    private long id;
    private boolean isValid;
    private long enrichedValue = -1L;


    public void setId(long id) {
        this.id = id;
    }

    @Override
    public long getId() {
        return id;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    public long getEnrichedValue() {
        return enrichedValue;
    }

    public void setEnrichedValue(long enrichedValue) {
        this.enrichedValue = enrichedValue;
    }
}
