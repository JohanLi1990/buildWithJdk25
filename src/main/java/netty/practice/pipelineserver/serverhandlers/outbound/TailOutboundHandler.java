package netty.practice.pipelineserver.serverhandlers.outbound;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
Outbound #1 – Tail logger

Class idea: OutboundLoggingHandler1

Purpose: log writes just before they reach the socket.

Behavior:

Override write(ctx, msg, promise):

Log "Outbound1.write: <msg>".

Call ctx.write(msg, promise).

Override flush(ctx):

Log "Outbound1.flush".

Call ctx.flush().

Attach this near the tail to see the end of outbound path.

 */
public class TailOutboundHandler extends ChannelOutboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TailOutboundHandler.class);

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        LOGGER.info("Outbound1.write: {}", msg);
        ctx.write(msg, promise);
    }

    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("Outbound1.flush...");
        ctx.flush();
    }
}
