package netty.practice.bytebuf;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ByteBufHandler2 extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ByteBufHandler2.class);
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf cur = (ByteBuf) msg;
        LOGGER.info("Different index: {}", cur);
        var duplicate = cur.duplicate();
        var slice = cur.slice(0, 5);
        var retainedSlice = cur.retainedSlice();
        logViews(duplicate, slice, retainedSlice);

        // mutate view
        duplicate.writeInt(5);
        logViews(duplicate, slice, retainedSlice);

        slice.resetWriterIndex();
        slice.writeInt(3);
        logViews(duplicate, slice, retainedSlice);

        retainedSlice.resetWriterIndex();
        retainedSlice.writeBytes(new byte[]{2, 4, 5});
        logViews(duplicate, slice, retainedSlice);

        // release ref check Ref counts
//        cur.release();
        LOGGER.info("Now we have released original bytebuf...");
        logViews(duplicate,slice,retainedSlice);

        cur.release();
        retainedSlice.release();
//        ctx.fireChannelRead(msg);

    }

    void logViews(ByteBuf duplicate, ByteBuf slice, ByteBuf retainedSlice) {
        LOGGER.info("Ref Count duplicate: {}, {}", duplicate, duplicate.refCnt());
        LOGGER.info("Ref Count slice: {}, {}", slice, slice.refCnt());
        LOGGER.info("Ref Count retainedSlice: {}, {}", retainedSlice, retainedSlice.refCnt());


    }
}
