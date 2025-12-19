package disruptor.practice.netty.events;

import disruptor.practice.common.IEvent;
import io.netty.channel.Channel;

public class NettyDisruptorEvent implements IEvent {
    private long id;
    private long t0;
    private long t1;
    private String payload;
    private Channel ch;

    public void setId(long id) {
        this.id = id;
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

    @Override
    public long getId() {
        return id;
    }

    public Channel getCh() {
        return ch;
    }

    public void setCh(Channel ch) {
        this.ch = ch;
    }

    public long getT1() {
        return t1;
    }

    public void setT1(long t1) {
        this.t1 = t1;
    }
}
