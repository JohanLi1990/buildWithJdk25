package netty.practice.channellifecycle;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class SimpleMessageHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleMessageHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        LOGGER.info("SimpleMessageHandler channel read....");

        switch (msg.toString(CharsetUtil.UTF_8)) {
            case "PING" -> happyPath(ctx, msg);
            case "BOOM" -> {
                LOGGER.error("Throwing exception");
                throw new RuntimeException("Exploded!");
            }
            default -> serverClose(ctx);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.info("Exception caught...");
        ctx.close();
    }

    private void serverClose(ChannelHandlerContext ctx) {
        LOGGER.error("Some errors... server closing...");
        ctx.writeAndFlush(Unpooled.copiedBuffer("Server encountered error".getBytes(StandardCharsets.UTF_8)));
        ctx.close();
    }

    private void happyPath(ChannelHandlerContext ctx, ByteBuf msg) {

        LOGGER.info("Respond....");
        var resp = ctx.alloc().buffer();
        resp.writeBytes("PONG".getBytes(CharsetUtil.UTF_8));
        ctx.writeAndFlush(resp);
    }
}
