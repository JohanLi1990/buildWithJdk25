package netty.practice.bytebuf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ByteBufHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ByteBufHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        LOGGER.info("is msg ByteBuf? : {}", msg instanceof ByteBuf);

        ByteBuf in = (ByteBuf) msg;
        LOGGER.info("Incoming text: {}", in.toString(CharsetUtil.UTF_8));
        LOGGER.info("Buf property, {}; \nis it Direct? {};\n has array ? {};\n who allocated it? {}", in, in.isDirect(),
                in.hasArray(), in.alloc().getClass().getSimpleName());
        in.resetReaderIndex();
        LOGGER.info("msg:{}, msg-refcount:{}, msg-readablebyte:{}, isDirect ? :{}", in, in.refCnt(),
                in.readableBytes(), in.isDirect());
        var intValue = in.getInt(0);
        LOGGER.info("current read index: {}", in);
        var realValue = in.readInt();

        // step 5, see that buffer can be allocated on heap.
        var unpooledHeap = Unpooled.buffer(32);
        unpooledHeap.writeBytes("I am the boss!".getBytes(CharsetUtil.UTF_8));
        LOGGER.info("UNpooledheap: {}, isDirect={}, hasArray={}", unpooledHeap, unpooledHeap.isDirect(),
                unpooledHeap.hasArray());
        unpooledHeap.release();




        ctx.fireChannelRead(msg);
    }
}
