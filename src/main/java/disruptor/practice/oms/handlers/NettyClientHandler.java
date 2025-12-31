package disruptor.practice.oms.handlers;

import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class NettyClientHandler extends ChannelDuplexHandler {

    private static final Logger log = LoggerFactory.getLogger(NettyClientHandler.class);
    private static final int LIMIT = 40_000; // 40k events
    private int maxNumOfTasks = 0;
    private int completed = 0;
    private Map<Integer, Integer> correlationSequence = new HashMap<>();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // send the data from here.
//        sendOneTask(ctx);
        sendManayTask(ctx);
    }

    private void sendOneTask(ChannelHandlerContext ctx) {
        //PAYLOAD example: 78840341|2|BUY 100 TSLA|1901390194
        String test = "78840342|3|2|Buy 100 Tsla\n";
        var cur = ctx.alloc().ioBuffer(ByteBufUtil.utf8MaxBytes(test));
        ByteBufUtil.writeUtf8(cur, test);
        ctx.writeAndFlush(cur);
    }

    private void sendManayTask(ChannelHandlerContext ctx) {
        while (ctx.channel().isWritable() && maxNumOfTasks < LIMIT) {
            String curMsg = createOneMessage(maxNumOfTasks++);
            var cur = ctx.alloc().ioBuffer(ByteBufUtil.utf8MaxBytes(curMsg));
            ByteBufUtil.writeUtf8(cur, curMsg);
            ctx.write(cur);
        }
        ctx.flush();
    }

    private String createOneMessage(int maxNumOfTasks) {
        int curOrderId = maxNumOfTasks + 13;
        int correlationId = curOrderId % 10;
        // send 10 family, each 1000 TaskEvents
        // after that send 1 family, with 30000 TaskEvents
        if (10_000 < maxNumOfTasks && maxNumOfTasks < LIMIT) {
            correlationId = 11; // TBR events
        }
        correlationSequence.put(correlationId, correlationSequence.getOrDefault(correlationId, 0) + 1);
        return curOrderId+"|"+correlationId+"|" + correlationSequence.get(correlationId) +"|Description\n";

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        completed++;
        if ((completed & 1023) == 0) {
            log.info("{} msg completed!", completed);
        }
        ReferenceCountUtil.release(msg);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().isWritable()) {
            sendManayTask(ctx);
        }
    }
}
