package disruptor.practice.netty.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;


public class ClientInboundHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientInboundHandler.class);
    public static final int LIMIT = 100000;
    private int count = 0;
    private int received = 0;
    private int rejected = 0;

    private static final String PAYLOAD_FAST = "挽狂澜于既倒，见广厦于废墟\n";
    private static final String PAYLOAD_SLOW = "知不可乎骤得，托遗响于悲风\n";
    private boolean slowLoopScheduled = false;
    private static final int size = ByteBufUtil.utf8MaxBytes(PAYLOAD_FAST) + 8; // Long is 8 bytes

    private final boolean slow;

    public ClientInboundHandler(boolean slow) {
        this.slow = slow;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("Channel Active!!.. start sending");
        if (slow) {
            startSlowLoopIfNeeded(ctx);
        } else {
            fastWriting(ctx);
        }
    }

    private void startSlowLoopIfNeeded(ChannelHandlerContext ctx) {
        if (slowLoopScheduled) return;
        slowLoopScheduled = true;
        scheduleSlowTick(ctx);
    }

    private void fastWriting(ChannelHandlerContext ctx) {
        while (ctx.channel().isWritable() && count < LIMIT) {
            ByteBuf cur = ctx.alloc().ioBuffer(size);
            ByteBufUtil.writeUtf8(cur, String.valueOf(count++));
            ByteBufUtil.writeUtf8(cur, PAYLOAD_FAST);
            ctx.write(cur);
        }
        ctx.flush();
    }

    private void scheduleSlowTick(ChannelHandlerContext ctx) {
        ctx.executor().schedule(() -> slowTick(ctx), 1, TimeUnit.MILLISECONDS);
    }

    private void slowTick(ChannelHandlerContext ctx) {
        try {
            if (!ctx.channel().isActive() || count >= LIMIT) {
                slowLoopScheduled = false;
                return;
            }

            if (!ctx.channel().isWritable()) {
                scheduleSlowTick(ctx);
                return;
            }

            // Send exactly ONE message per tick (true pacing)
            ByteBuf cur = ctx.alloc().ioBuffer(size);
            ByteBufUtil.writeUtf8(cur, String.valueOf(count++));
            ByteBufUtil.writeUtf8(cur, PAYLOAD_SLOW);
            ctx.writeAndFlush(cur);

            scheduleSlowTick(ctx);
        } catch (Throwable t) {
            // Safety: make sure flag resets if something unexpected happens.
            slowLoopScheduled = false;
            throw t;
        }
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) {
        boolean writable = ctx.channel().isWritable();
        LOGGER.info("channel writable ? {}", writable);

        if (!writable) return;

        if (slow) {
            startSlowLoopIfNeeded(ctx);
        } else {
            fastWriting(ctx);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            String s = ((ByteBuf) msg).toString(CharsetUtil.UTF_8);
//            LOGGER.info("Received echo: {}", s);
            received++;
            if (s.startsWith("REJ")) rejected++;
            if (received == LIMIT) {
                LOGGER.info("Done, received={}, rejected={}", received, rejected);
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }
}
