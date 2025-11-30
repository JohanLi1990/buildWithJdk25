package netty.practice.backpressure;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BurstClientHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(BurstClientHandler.class);
    private ChannelFuture lastWriteFuture = null;

    private int nextIndex = 0;
    private final int totalMessages = 1000;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        long startNanos = System.nanoTime();
        helper(ctx);
        lastWriteFuture.addListener(f -> {
           long endNano = System.nanoTime();
           long durationMilli = (endNano - startNanos) / 1_000_000;
           LOGGER.info("All writes finished in {} ms", durationMilli);
        });
    }

    private void helper(ChannelHandlerContext ctx) {
        String payload = "MSG-";
        LOGGER.info("{} Start sending message, current index {}...", System.currentTimeMillis(), nextIndex);
        while(ctx.channel().isWritable() && nextIndex < totalMessages) {
            String req = payload + nextIndex++ + System.getProperty("line.separator");
            lastWriteFuture =  ctx.write(Unpooled.copiedBuffer(req, CharsetUtil.UTF_8));
        }
        ctx.flush();
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("Client Channel Inactive...");
        super.channelInactive(ctx);
    }



    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("isWritable={}", ctx.channel().isWritable());
        if (ctx.channel().isWritable() && nextIndex < totalMessages) {
            helper(ctx);
        }
    }
}
