package netty.practice.pipelineserver.serverhandlers.outbound;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
Outbound #2 – Modifier (e.g. encoder)

Class idea: OutboundTransformHandler2

Purpose: pretend to be an encoder.

Behavior:

In write, log "Outbound2.write BEFORE: <msg>".

Transform the message (e.g. add prefix "OUT2-").

Log "Outbound2.write AFTER: <msg'>".

Call ctx.write(transformed, promise).
 */
public class ModifierHandler extends ChannelOutboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModifierHandler.class);

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        LOGGER.info("Outbound2.write BEFORE: {}", msg);
        ByteBuf out = (ByteBuf)msg;
        String outString = out.toString();
        outString = "OUT2 - " + outString;
        LOGGER.info("Outbound2.write AFTER: {}", msg);
        ctx.write(outString, promise);
    }
}
