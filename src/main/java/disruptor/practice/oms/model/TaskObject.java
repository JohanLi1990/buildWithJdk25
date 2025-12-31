package disruptor.practice.oms.model;

import io.netty.channel.Channel;

public class TaskObject {

    private final long orderId;
    private final int correlationId;
    private final int seqInFamily;
    private final Channel channel;
    private final boolean isEob;
    private final String payload;

    public TaskObject(long orderId, int correlationId, int seqInFamily, Channel channel, boolean isEob, String payload) {
        this.orderId = orderId;
        this.correlationId = correlationId;
        this.seqInFamily = seqInFamily;
        this.channel = channel;
        this.isEob = isEob;
        this.payload = payload;
    }

    public TaskObject(TaskEvent te, boolean isEob) {
        this.orderId = te.getOrderId();
        this.correlationId = te.getCorrelationId();
        this.channel = te.getChannel();
        this.seqInFamily = te.getSeqInFamily();
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

    public int getSeqInFamily() {
        return seqInFamily;
    }
}