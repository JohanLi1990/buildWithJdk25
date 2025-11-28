package netty.practice.pipelineserver.serverhandlers.outbound;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
Outbound #3 – Buffer / delay flusher

Class idea: OutboundBufferingHandler3

Purpose: show difference between write and flush.

Behavior (simple version):

In write, log "Outbound3.write: <msg>", then ctx.write(msg, promise) (no magic).

In flush, log "Outbound3.flush", then ctx.flush().

If you want to play more later, you can buffer writes and only flush under certain conditions – but for now, logging is enough.

This shows: write() just enqueues, flush() actually pushes to socket.
 */
public class BufferingHandler extends ChannelOutboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(BufferingHandler.class);

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        LOGGER.info("Outbound3.write: {}", msg);
        ctx.write(msg, promise);
    }

    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("Outbound3.flush....");
        ctx.flush();
    }
}
