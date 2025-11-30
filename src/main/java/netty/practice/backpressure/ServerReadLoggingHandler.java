package netty.practice.backpressure;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerReadLoggingHandler extends ChannelDuplexHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerReadLoggingHandler.class);
    private int channelReadCount;
    private int readCount;
    private int channelReadCompleteCount;

    @Override
    public void read(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("Outbound read triggered {} times", readCount++);
        super.read(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        var buf = (ByteBuf) msg;
        LOGGER.info("Channel recieved: {}; ChannelRead triggered {} times", buf.toString(CharsetUtil.UTF_8), ++channelReadCount);
        super.channelRead(ctx, msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("ChannelReadComplete triggered {} times", ++channelReadCompleteCount);
        super.channelReadComplete(ctx);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) {
        LOGGER.info("Is current ctx channel writable? {}", ctx.channel().isWritable());
    }
}
