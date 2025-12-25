package disruptor.practice.oms.handlers.codec;

import disruptor.practice.oms.model.TaskResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

public class NettyOrderEventEncoder extends MessageToMessageEncoder<TaskResponse> {

    @Override
    protected void encode(ChannelHandlerContext ctx, TaskResponse msg, List<Object> out) throws Exception {
        String outMsg = msg.toString();
        int size = ByteBufUtil.utf8MaxBytes(outMsg) + ByteBufUtil.utf8MaxBytes("\n");
        ByteBuf cur = ctx.alloc().ioBuffer(size);
        ByteBufUtil.writeUtf8(cur, outMsg);
        ByteBufUtil.writeUtf8(cur, "\n");
        out.add(cur);
    }
}
