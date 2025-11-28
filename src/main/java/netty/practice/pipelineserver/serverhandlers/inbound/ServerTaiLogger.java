package netty.practice.pipelineserver.serverhandlers.inbound;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Class idea: InboundTerminalHandler4

Purpose: see what happens at the end of the inbound chain.

Behavior:

On channelRead, log "Inbound4.channelRead: <msg>".

You can try two versions:

Forward: call ctx.fireChannelRead(msg) and see if anything happens after.

Stop: do not call fireChannelRead and see that the event stops here.
 */
public class ServerTaiLogger extends SimpleChannelInboundHandler<ByteBuf> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerTaiLogger.class);
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        LOGGER.info("Inbound4.channelRead: {}", msg.toString(CharsetUtil.UTF_8));
        ctx.fireChannelRead(msg);
    }
}
