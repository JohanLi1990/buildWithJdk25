package netty.practice.pipelineserver.serverhandlers.inbound;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class idea: InboundBusinessHandler3
 *
 * Purpose: pretend to be your business logic layer.
 *
 * Behavior:
 *
 * On channelRead, log "Inbound3.channelRead: <msg>".
 *
 * Maybe trigger an outbound write: ctx.writeAndFlush(response) (e.g. "OK from handler3").
 *
 * Then call ctx.fireChannelRead(msg) so downstream handlers still see it.
 */
public class ServerBusinessHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static Logger LOGGER = LoggerFactory.getLogger(ServerBusinessHandler.class);


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        LOGGER.info("Apply Business Logic to {}", msg.toString(CharsetUtil.UTF_8));
        ctx.writeAndFlush(Unpooled.copiedBuffer("Business thinks its okay", CharsetUtil.UTF_8));
        ctx.fireChannelRead(msg.retain());
    }
}
