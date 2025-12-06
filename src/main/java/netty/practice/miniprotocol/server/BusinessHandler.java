package netty.practice.miniprotocol.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;
import netty.practice.miniprotocol.models.AckMessage;
import netty.practice.miniprotocol.models.Message;
import netty.practice.miniprotocol.models.RejMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BusinessHandler extends SimpleChannelInboundHandler<Message> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessHandler.class);
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        LOGGER.info("Business Handling of {}", msg);
        if (msg instanceof RejMessage rej) {
            ctx.writeAndFlush(rej);
            return;
        }
        ctx.writeAndFlush(new AckMessage(20));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("Cannot read message {}", cause.getMessage());
        var out = ctx.alloc().buffer();
        out.writeBytes( cause.getMessage().getBytes(CharsetUtil.UTF_8));
        ctx.writeAndFlush(out);
        ctx.close();
    }
}