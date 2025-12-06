package netty.practice.miniprotocol.codec;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.CharsetUtil;
import netty.practice.miniprotocol.models.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class FixMessageEncoder extends MessageToMessageEncoder<Message> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, List<Object> out) throws Exception {
        String cur = "";
        if (msg instanceof RejMessage) {
            cur = "REJ|"+ ((RejMessage) msg).reason();
        } else if (msg instanceof AckMessage) {
            cur = "ACK|" + ((AckMessage)msg).id();
        } else if (msg instanceof CancelOrderMessage) {
            cur = "CXL|" + ((CancelOrderMessage)msg).getId();
        } else if (msg instanceof Ping) {
            cur = "PING";
        } else if (msg instanceof NewOrderMessage) {
            var newMsg = (NewOrderMessage) msg;
            cur = "NEW|" + newMsg.getId() + "|" + newMsg.getQty() + "|" + newMsg.getSide();
        }

        if (!cur.isEmpty()) {
            cur += System.getProperty("line.separator");
            var curBuf = ctx.alloc().buffer();
            curBuf.writeBytes(cur.getBytes(CharsetUtil.UTF_8));
            out.add(curBuf);
        }
    }
}
