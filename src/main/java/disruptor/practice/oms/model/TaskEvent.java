package disruptor.practice.oms.model;

import io.netty.channel.Channel;

public class TaskEvent {

    // mandatory fields
    private long t0;// timestamp when msg received on Netty thread
    // Different tasks might be about the same orders, (CANCEL, AMEND, NEW, ACCEPT...)
    private long orderId;
    // correlationId
    private int seqInFamily;
    private int correlationId;
    private String payload;
    private Channel channel;
    private TaskType taskType;
    private String rejectReason;

    public TaskEvent() {}

    public TaskEvent(long t0, long orderId, int seqInFamily, int correlationId, String payload, Channel channel) {
        this.t0 = t0;
        this.orderId = orderId;
        this.seqInFamily = seqInFamily;
        this.correlationId = correlationId;
        this.payload = payload;
        this.channel = channel;
        this.taskType = TaskType.NORMAL;
        this.rejectReason = "";
    }

    public TaskEvent(TaskType taskType, String payload, String reason, Channel channel) {
        this.t0 = -1;
        this.orderId = -1;
        this.seqInFamily = -1;
        this.correlationId = -1;
        this.payload = payload;
        this.channel = channel;
        this.taskType = taskType;
        this.rejectReason = reason;
    }


    public long getT0() {
        return t0;
    }

    public void setT0(long t0) {
        this.t0 = t0;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public long getOrderId() {
        return orderId;
    }

    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }

    public int getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(int correlationId) {
        this.correlationId = correlationId;
    }

    @Override
    public String toString() {
        return "TaskEvent{" +
                "t0=" + t0 +
                ", orderId=" + orderId +
                ", seqInFamily=" + seqInFamily +
                ", correlationId=" + correlationId +
                ", payload='" + payload + '\'' +
                ", channel=" + channel +
                ", taskType=" + taskType +
                ", rejectReason='" + rejectReason + '\'' +
                '}';
    }

    public void clone(TaskEvent another) {
        this.t0 = another.t0;
        this.channel = another.channel;
        this.orderId = another.orderId;
        this.correlationId = another.getCorrelationId();
        this.payload = another.payload;
        this.setSeqInFamily(another.getSeqInFamily());
        this.taskType = another.getTaskType();
        this.rejectReason = another.getRejectReason();
    }

    public int getSeqInFamily() {
        return seqInFamily;
    }

    public void setSeqInFamily(int seqInFamily) {
        this.seqInFamily = seqInFamily;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }
}


