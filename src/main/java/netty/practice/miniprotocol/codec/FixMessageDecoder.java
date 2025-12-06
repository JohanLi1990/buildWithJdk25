package netty.practice.miniprotocol.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.CharsetUtil;
import netty.practice.miniprotocol.models.MessageFactory;
import netty.practice.miniprotocol.models.RejMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FixMessageDecoder extends MessageToMessageDecoder<ByteBuf> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FixMessageDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        String rawMsg = msg.toString(CharsetUtil.UTF_8);
        String[] fields = rawMsg.split("\\|");
        try {
            var curMsg = MessageFactory.createMessage(fields);
            out.add(curMsg);
        } catch (Exception e) {
            LOGGER.error("Malformed input, unable to create message from {}", rawMsg);
//            throw new DecoderException("Malformed messages: " + rawMsg);
            out.add(new RejMessage("Malformed Input!"));
        }

    }

}
