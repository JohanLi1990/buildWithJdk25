package disruptor.practice.oms.handlers;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import disruptor.practice.oms.model.TaskEvent;
import disruptor.practice.oms.model.TaskResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.Math.floorMod;

/**
 *  For handing TaskEvent off to Disruptors
 *  TaskEvent routing happens in this class as well
 */

public class NettyIOHandler extends SimpleChannelInboundHandler<TaskEvent> {

    // Number of Partitions:
    private final int N;

    private final List<Disruptor<TaskEvent>> disruptors;
    private static final String SERVER_BUSY = "REJ:BUSY";

    private long totalCounts = 0L;
    private long rejectsBusy = 0L;
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyIOHandler.class);

    public NettyIOHandler(List<Disruptor<TaskEvent>> disruptors) {
        this.disruptors = disruptors;
        N = disruptors.size();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TaskEvent msg) throws Exception {
        totalCounts++;
//        LOGGER.info("published: {}, rejected:{}", totalCounts - rejectsBusy, rejectsBusy);
        if ((totalCounts & 255) == 0) {
            LOGGER.info("published: {}, rejected:{}", totalCounts - rejectsBusy, rejectsBusy);
        }
        // compute partitions
        int correlationId = msg.getCorrelationId();
//        int partition = correlationId < 0 ? floorMod(System.identityHashCode(ctx.channel()), N):
        int partition = correlationId < 0 ? floorMod(msg.getOrderId(), N): floorMod(correlationId, N);
                //
        RingBuffer<TaskEvent> ringBuffer = disruptors.get(partition).getRingBuffer();
        if (!ringBuffer.tryPublishEvent(NettyIOHandler::translate, msg)) {
            // failed to publish
            rejectsBusy++;
            ctx.channel().eventLoop().execute(() -> ctx.writeAndFlush(new TaskResponse(msg.getOrderId(), correlationId,
                    partition,
                    msg.getSeqInFamily(),
                    SERVER_BUSY,
                    -1, -1, msg.getPayload())));
            LOGGER.info("Failed to publish to Disruptor msg: {}", msg);
        }
    }

    static void translate(TaskEvent event, long seq, TaskEvent in) {
        event.clone(in);
    }
}
