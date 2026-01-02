package disruptor.practice.oms.handlers.codec;

import disruptor.practice.oms.model.TaskEvent;
import disruptor.practice.oms.model.TaskType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class NettyOrderEventDecoder extends MessageToMessageDecoder<ByteBuf> {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyOrderEventDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        String rawInput = msg.toString(CharsetUtil.UTF_8);
        String[] fields = rawInput.split("\\|");
        try {
            TaskEvent res = new TaskEvent();
            // PAYLOAD example: 78840341|2|99|BUY 100 TSLA
            res.setOrderId(Long.parseLong(fields[0]));
            res.setCorrelationId(Integer.parseInt(fields[1]));
            res.setSeqInFamily(Integer.parseInt(fields[2]));
            res.setPayload(fields[3]);
            res.setT0(System.nanoTime());
            res.setChannel(ctx.channel());
            out.add(res);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            // Do not write back immediately!
            // Make the write back path unique (via the Disruptor)
//            new TaskResponse(-1, -1, -1, -1,BAD_MSG + rawInput, System.nanoTime());
            TaskEvent badMessage = new TaskEvent(TaskType.INVALID, rawInput, "BAD MESSAGE", ctx.channel());
            badMessage.setT0(System.nanoTime());
            out.add(badMessage);
            // propagate from the tail
            LOGGER.debug("Failed to parse Order: {}", rawInput);
        }
    }
}
