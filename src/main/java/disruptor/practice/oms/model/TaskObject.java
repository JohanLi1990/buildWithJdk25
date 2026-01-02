package disruptor.practice.oms.model;

import io.netty.channel.Channel;

public class TaskObject {

    private final long orderId;
    private final int correlationId;
    private final int seqInFamily;
    private final Channel channel;
    private final boolean isEob;
    private final String payload;
    private final TaskType taskType;
    private final String rejReason;
    private final long t0;

    public TaskObject(long orderId, int correlationId, int seqInFamily, Channel channel, boolean isEob,
                      String payload, long t0) {
        this.orderId = orderId;
        this.correlationId = correlationId;
        this.seqInFamily = seqInFamily;
        this.channel = channel;
        this.isEob = isEob;
        this.payload = payload;
        this.taskType = TaskType.NORMAL;
        this.rejReason=null;
        this.t0 = t0;
    }

    public TaskObject(TaskEvent te, boolean isEob) {
        this.orderId = te.getOrderId();
        this.correlationId = te.getCorrelationId();
        this.channel = te.getChannel();
        this.seqInFamily = te.getSeqInFamily();
        this.isEob = isEob;
        this.payload = te.getPayload();
        this.taskType = te.getTaskType();
        this.rejReason = te.getRejectReason();
        this.t0 = te.getT0();
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

    public TaskType getTaskType() {
        return taskType;
    }

    public String getRejReason() {
        return rejReason;
    }

    public long getT0() {
        return t0;
    }
}