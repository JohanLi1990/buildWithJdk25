package netty.practice.channellifecycle;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleChannelWriteHandler extends ChannelOutboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleChannelWriteHandler.class);

    @Override
    public void flush(ChannelHandlerContext ctx) {
        LOGGER.error("Something happened in flush....");
        throw new RuntimeException("Flush encountered exception");
    }
}
