package netty.practice.pipelineserver.serverhandlers.inbound;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;


/**
 * Class idea: InboundTransformHandler2
 * Purpose: pretend to be a decoder or pre-processor.
 * Behavior:
 * On channelRead, log "Inbound2.channelRead BEFORE: <msg>".
 * “Transform” the message (e.g., ByteBuf → String, or uppercase a String).
 * Log "Inbound2.channelRead AFTER: <msg'>".
 * Call ctx.fireChannelRead(transformedMsg).
 */
public class ServerTransformHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerTransformHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        LOGGER.info("Inbound2.ChannelRead, BEFORE: {}", msg.toString(CharsetUtil.UTF_8));
        String transformed = msg.toString(CharsetUtil.UTF_8).toUpperCase();
        LOGGER.info("Inbound2.ChannelRead AFTER: {}", transformed);
        ctx.fireChannelRead(Unpooled.copiedBuffer(transformed, StandardCharsets.UTF_8).retain());
    }
}
