package netty.practice.channellifecycle;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

public class SimpleClientSend extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx)  {
        var msg = ctx.alloc().buffer();
        msg.writeBytes("PING".getBytes(CharsetUtil.UTF_8));
        ctx.writeAndFlush(msg);
    }
}
