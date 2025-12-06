package netty.practice.miniprotocol.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

import java.nio.charset.StandardCharsets;

public class ClientHandler extends ChannelInboundHandlerAdapter {


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
//        partialPacketTest(ctx);
//        stickyPacketTest(ctx);
        malformedInput(ctx);
    }

    private void malformedInput(ChannelHandlerContext ctx) {
        var out = ctx.alloc().buffer();
        out.writeBytes("NEW|xxx|2|uhu\n".getBytes(CharsetUtil.UTF_8));
        ctx.writeAndFlush(out);
    }

    private void stickyPacketTest(ChannelHandlerContext ctx) {
        var out = ctx.alloc().buffer();
        var msg = "NEW|1|20|SELL" + System.getProperty("line.separator") + "CXL|1" + System.getProperty("line.separator");
        out.writeBytes(msg.getBytes(StandardCharsets.UTF_8));
        ctx.writeAndFlush(out);
    }

    private static void partialPacketTest(ChannelHandlerContext ctx) throws InterruptedException {
        var out = ctx.alloc().buffer();
        var curMessage = "NEW|1|20|SEL";
        // first send
        out.writeBytes(curMessage.getBytes(CharsetUtil.UTF_8));
        ctx.writeAndFlush(out);

        Thread.sleep(5000);
        // second send
        var out2 = ctx.alloc().buffer();
        out2.writeBytes(("L" + System.getProperty("line.separator")).getBytes(CharsetUtil.UTF_8));
        ctx.writeAndFlush(out2);
    }

}
