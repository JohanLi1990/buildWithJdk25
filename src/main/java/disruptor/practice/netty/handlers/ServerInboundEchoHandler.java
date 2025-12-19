package disruptor.practice.netty.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerInboundEchoHandler extends ChannelInboundHandlerAdapter {
    private final static Logger LOGGER = LoggerFactory.getLogger(ServerInboundEchoHandler.class);

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("Channel Active...");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        LOGGER.info("Buffer Type: {}; Readable Bytes Count: {}; first 5 bytes: {}",msg.getClass().getSimpleName(),
                ((ByteBuf)msg).readableBytes(), ((ByteBuf)msg).readBytes(5).toString(CharsetUtil.UTF_8));
        ctx.fireChannelRead(((ByteBuf)msg).resetReaderIndex());
        ctx.writeAndFlush(msg);
    }
}
