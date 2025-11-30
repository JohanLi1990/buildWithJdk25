package netty.practice.backpressure;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;

public class ServerSlowBusinessHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerSlowBusinessHandler.class);
    private int count = 0;
    private final ExecutorService workerpool;
    private final Queue<String> queue;
    private boolean processing;

    public ServerSlowBusinessHandler(ExecutorService workerpool) {
        super();
        this.workerpool = workerpool;
        this.queue = new LinkedList<>();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        LOGGER.info("Channel Active, Current thread name: {}; Current auto-read setting: {}",
                Thread.currentThread().getName(), ctx.channel().config().isAutoRead());
        ctx.read();
        LOGGER.info("Requested first read...");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf in = (ByteBuf) msg;
        String input = in.toString(CharsetUtil.UTF_8);
        LOGGER.info("Message content: {}, Timestamp: {}; Size of the buffer: {}; Message Count: {}",
                input, System.currentTimeMillis(), in.readableBytes(), ++count);

        // 200 ms business processing
//        naiveApproach();
        betterApproach(ctx, input);
        in.release();

    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("ChannelRead completes! There are {} channelRead; NOT calling ctx.read() here...", count);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("Exception caught: {}", cause.getMessage());
        ctx.close();
    }

    private void betterApproach(ChannelHandlerContext ctx, String in) {
        this.queue.add(in);
        if (!processing) {
            processing = true;
            workerpool.submit(() -> {
                while (true) {
                    if (!queue.isEmpty()) {
                        String cur = queue.poll();
                        Thread.sleep(200);
                        LOGGER.info("{} processed by {}", cur, Thread.currentThread().getName());
                        ctx.executor().execute(ctx::read);
                    } else {
                        processing = false;
                    }
                }
            });

        }

    }

    private void naiveApproach() throws InterruptedException {
        LOGGER.info("(Naive Approach) Handling Business...");
        Thread.sleep(200);
    }
}
