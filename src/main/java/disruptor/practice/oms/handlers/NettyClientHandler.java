package disruptor.practice.oms.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyClientHandler extends ChannelDuplexHandler {

    private static final Logger log = LoggerFactory.getLogger(NettyClientHandler.class);

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // send the data from here.
        sendOneTask(ctx);
    }

    private void sendOneTask(ChannelHandlerContext ctx) {
        //PAYLOAD example: 78840341|2|BUY 100 TSLA|1901390194
        String test = "78840342|3|Buy 100 Tsla\n";
        var cur = ctx.alloc().ioBuffer(ByteBufUtil.utf8MaxBytes(test));
        ByteBufUtil.writeUtf8(cur, test);
        ctx.writeAndFlush(cur);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String in = ((ByteBuf)msg).toString(CharsetUtil.UTF_8);
        log.info("Response: {}", in);
        ReferenceCountUtil.release(msg);
    }
}
