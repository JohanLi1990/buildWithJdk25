package netty.practice.pipelineserver.serverhandlers.inbound;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerHeadLogger extends SimpleChannelInboundHandler<ByteBuf> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerHeadLogger.class);

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("In bound ChannelActive");
        ctx.fireChannelActive();
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        LOGGER.info("Inbound 1 Channel Active : {}", msg.toString(CharsetUtil.UTF_8));
        ctx.fireChannelRead(msg.retain());
    }
}
