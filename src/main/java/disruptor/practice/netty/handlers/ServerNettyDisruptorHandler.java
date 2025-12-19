package disruptor.practice.netty.handlers;

import com.lmax.disruptor.InsufficientCapacityException;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import disruptor.practice.netty.events.NettyDisruptorEvent;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
Source of truth
 */
public class ServerNettyDisruptorHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerNettyDisruptorHandler.class);
    private final Disruptor<NettyDisruptorEvent> disruptor;
    private final static String REJ_PREFIX = "REJ";
    private static int count = 0;

    public ServerNettyDisruptorHandler(Disruptor<NettyDisruptorEvent> disruptor) {
        this.disruptor = disruptor;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        long t0 = System.nanoTime(); // full frame
        String fullMessage = ((ByteBuf) msg).toString(CharsetUtil.UTF_8);
        // hands off to disruptor;
        publishToDisruptor(ctx , disruptor, fullMessage, t0);
        ReferenceCountUtil.release(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("Caught exception: {}", cause.getMessage());
        cause.printStackTrace();
        ctx.close();
    }

    private void publishToDisruptor(ChannelHandlerContext ctx , Disruptor<NettyDisruptorEvent> disruptor,
                                    String fullMsg, long t0) {
        RingBuffer<NettyDisruptorEvent> ringBuffer = disruptor.getRingBuffer();
        long seq = -1L;
        try {
            seq = ringBuffer.tryNext();
            NettyDisruptorEvent event = ringBuffer.get(seq);
            translateTo(event, seq, t0, fullMsg, 0L /* place holder*/, ctx.channel());
            long t1 = System.nanoTime();
            event.setT1(t1);
            LOGGER.info("Hand Off Prep cost: {}", t1 - t0);
        } catch (InsufficientCapacityException e) {
            LOGGER.error("Disruptor Busy, forced to reject event {}, rejected {} msg!", fullMsg, count++);
            int size = ByteBufUtil.utf8MaxBytes(REJ_PREFIX) + ByteBufUtil.utf8Bytes("BUSY\n");
            ByteBuf cur = ctx.alloc().ioBuffer(size); // ioBuffer() tends to pick direct when appropriate
            ByteBufUtil.writeUtf8(cur, REJ_PREFIX);
            ByteBufUtil.writeUtf8(cur, "BUSY\n");
            ctx.writeAndFlush(cur);
        } finally {
            if (seq != -1L) {
                ringBuffer.publish(seq);
            }
        }

    }

    public static void translateTo(NettyDisruptorEvent event, long sequence, long arg0, String arg1, long arg2,
                                   Channel channel) {
        event.setId(sequence);
        event.setPayload(arg1);
        event.setT0(arg0);
        event.setT1(arg2);
        event.setCh(channel);
    }

}
