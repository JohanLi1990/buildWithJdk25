package disruptor.practice.oms.model;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

public class TaskEvent {

    // mandatory fields
    private long t0;// timestamp when msg received on Netty thread
    // Different tasks might be about the same orders, (CANCEL, AMEND, NEW, ACCEPT...)
    private long orderId;
    // correlationId
    private int correlationId;
    private String payload;
    private Channel channel;


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
                ", correlationId=" + correlationId +
                ", payload=" + payload +
                ", channel=" + channel +
                '}';
    }

    public void clone(TaskEvent another) {
        this.t0 = another.t0;
        this.channel = another.channel;
        this.orderId = another.orderId;
        this.correlationId = another.getCorrelationId();
        this.payload = another.payload;
    }


}


