package netty.practice.echoserver;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EchoClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private String largetPayload = """
            孙子曰：兵者，国之大事，死生之地，存亡之道，不可不察也。
            
            故经之以五事，校之以计，而索其情：一曰道，二曰天，三曰地，四曰将、五曰法。道者，令民与上同意也，故可以与之死，可以与之生，而不畏危。
            天者，阴阳，寒暑、时制也。地者，远近、险易、广狭、死生也。将者，智、信、仁、勇、严也。法者，曲制、官道、主用也。凡此五者，将莫不闻，知之者胜，不知者不胜。
            故校之以计，而索其情，曰：主孰有道？将孰有能？天地孰得？法令孰行？兵众孰强？士卒孰练？赏罚孰明？吾以此知胜负矣。
            
            将听吾计，用之必胜，留之；将不听吾计，用之必败，去之。计利以听，乃为之势，以佐其外。势者，因利而制权也。兵书战策
            
            兵者，诡道也。故能而示之不能，用而示之不用，近而示之远，远而示之近；利而诱之，乱而取之，实而备之，强而避之，怒而挠之，卑而骄之，佚而劳之，亲而离之。攻其无备，出其不意。此兵家之胜，不可先传也。
            """ ;

    private static final Logger LOGGER = LoggerFactory.getLogger(EchoClientHandler.class);
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        LOGGER.info("client Accept: {} " , msg.toString(CharsetUtil.UTF_8));
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx)  {
//        for(int i = 0; i < 10; i++) {
        LOGGER.info("Sending data...");
        ctx.writeAndFlush(Unpooled.copiedBuffer(largetPayload, CharsetUtil.UTF_8));
//        }
//        ctx.writeAndFlush(largetPayload);
//        super.channelActive(ctx);
//        ctx.alloc().buffer();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("Exceptions caused {}", cause.toString());
        ctx.close();
    }
}
