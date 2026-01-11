package disruptor.practice.oms.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class NettyClientHandler extends ChannelDuplexHandler {

    private static final Logger log = LoggerFactory.getLogger(NettyClientHandler.class);
    private static final int LIMIT_HOT = 120_000; // 40k events
    private static final int LIMIT_COLD = 600;
    private int hotTask = 0;
    private int coldTask = 0;
    private int completed = 0;
    private Map<Integer, Integer> correlationSequence = new HashMap<>();
    private static final String DEFAULT_PAYLOAD = "BUY 100 TSLA";
    private static final String ILLEGAL_PAYLOAD = "BUY 100 BITCOIN";
    private static final String WRONG_PAYLOAD = "";
    private static final Random random = new Random();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // send the data from here.
//        sendOneTask(ctx);
//        sendManayTask(ctx);
        // HOT
        scheduleNext(ctx, 11, 500_000); // 2000 msg/s
        scheduleNext(ctx, 1000, 1049, 1_000_000_000);
    }

    private void sendOneTask(ChannelHandlerContext ctx) {
        //PAYLOAD example: 78840341|2|BUY 100 TSLA|1901390194
        String test = "78840342|3|2|Buy 100 Tsla\n";
        var cur = ctx.alloc().ioBuffer(ByteBufUtil.utf8MaxBytes(test));
        ByteBufUtil.writeUtf8(cur, test);
        ctx.writeAndFlush(cur);
    }

    private void sendOneTask(ChannelHandlerContext ctx, int corrId, int msgId) {
        //PAYLOAD example: 78840341|2|BUY 100 TSLA|1901390194
        String test = createOneMessage(corrId, msgId);
        var cur = ctx.alloc().ioBuffer(ByteBufUtil.utf8MaxBytes(test));
        ByteBufUtil.writeUtf8(cur, test);
        ctx.write(cur);
    }

    private void sendManayTask(ChannelHandlerContext ctx) {
        while (ctx.channel().isWritable() && hotTask < LIMIT_HOT) {
            String curMsg = createOneMessage(hotTask++, 11);
            var cur = ctx.alloc().ioBuffer(ByteBufUtil.utf8MaxBytes(curMsg));
            ByteBufUtil.writeUtf8(cur, curMsg);
            ctx.write(cur);
        }
        ctx.flush();
    }

    private void scheduleNext(ChannelHandlerContext ctx, int corrId, long intervalNs) {
        ctx.executor().schedule(() -> {
            if (hotTask >= LIMIT_HOT) {
                ctx.flush();
                return;
            }
            if (ctx.channel().isWritable()) {
                sendOneTask(ctx, corrId, hotTask++);
            }
            ctx.flush();
            scheduleNext(ctx, corrId, intervalNs);

        }, intervalNs, TimeUnit.NANOSECONDS);
    }

    private void scheduleNext(ChannelHandlerContext ctx, int fromCorrId, int toCorrId, long intervalNs) {
        ctx.executor().schedule(() -> {
            if (coldTask >= LIMIT_COLD) {
                ctx.flush();
                return;
            }
            if (ctx.channel().isWritable()) {
                for (int i = 0; i < 5; i++) {
                    sendOneTask(ctx, i * 8 + fromCorrId + 3, coldTask++);
                }
            }
            ctx.flush();
            scheduleNext(ctx, fromCorrId, toCorrId, intervalNs);


        }, intervalNs, TimeUnit.NANOSECONDS);
    }

    private void sendBurst(ChannelHandlerContext ctx, int cusCorrId) {
        while (ctx.channel().isWritable() && hotTask < LIMIT_HOT) {
            String curMsg = createOneMessage(hotTask++, cusCorrId);
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
        if (10_000 < maxNumOfTasks && maxNumOfTasks < LIMIT_HOT) {
            correlationId = 11; // TBR events
        }
        correlationSequence.put(correlationId, correlationSequence.getOrDefault(correlationId, 0) + 1);
        StringBuilder res = new StringBuilder();
        String actualPayLoad = switch (maxNumOfTasks % 7) {
            case 0 -> ILLEGAL_PAYLOAD;
            case 3 -> WRONG_PAYLOAD;
            default -> DEFAULT_PAYLOAD;
        };
        res.append(curOrderId).append("|")
                .append(correlationId).append("|")
                .append(correlationSequence.get(correlationId)).append("|")
                .append(actualPayLoad).append("\n");
        return res.toString();
    }

    private String createOneMessage(int customCorrelationId, int msgId) {
        int curOrderId = msgId;
        int correlationId = customCorrelationId;

        correlationSequence.put(correlationId, correlationSequence.getOrDefault(correlationId, 0) + 1);
        StringBuilder res = new StringBuilder();
        String actualPayLoad = DEFAULT_PAYLOAD;
        res.append(curOrderId).append("|")
                .append(correlationId).append("|")
                .append(correlationSequence.get(correlationId)).append("|")
                .append(actualPayLoad).append("\n");
        return res.toString();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.debug("{}", ((ByteBuf) msg).toString(CharsetUtil.UTF_8));
        completed++;
        if ((completed & 1023) == 0) {
            log.info("{} msg completed!", completed);
        }
        ReferenceCountUtil.release(msg);
    }

//    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        log.info("hot{}, cold{}", hotTask, coldTask);
    }
}
