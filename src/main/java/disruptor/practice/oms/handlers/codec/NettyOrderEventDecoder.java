package disruptor.practice.oms.handlers.codec;

import disruptor.practice.oms.model.TaskEvent;
import disruptor.practice.oms.model.TaskResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class NettyOrderEventDecoder extends MessageToMessageDecoder<ByteBuf> {
    private static final String BAD_MSG = "Bad Message";
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyOrderEventDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        String rawInput = msg.toString(CharsetUtil.UTF_8);
        String[] fields = rawInput.split("\\|");
        TaskEvent res = new TaskEvent();
        try {
            // PAYLOAD example: 78840341|2|BUY 100 TSLA
            res.setOrderId(Long.parseLong(fields[0]));
            res.setCorrelationId(Integer.parseInt(fields[1]));
            res.setPayload(fields[2]);
            res.setT0(System.nanoTime());
            res.setChannel(ctx.channel());
            out.add(res);
        } catch (NumberFormatException e) {
            // write back immediately this is rejected!
            var resp = new TaskResponse(-1, -1, BAD_MSG, System.nanoTime(), rawInput);
            // propagate from the tail
            ctx.channel().writeAndFlush(resp);
            LOGGER.debug("Failed to parse Order: {}", rawInput);
        }
    }
}
