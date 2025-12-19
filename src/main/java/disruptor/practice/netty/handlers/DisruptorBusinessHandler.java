package disruptor.practice.netty.handlers;

import com.lmax.disruptor.EventHandler;
import disruptor.practice.netty.events.NettyDisruptorEvent;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DisruptorBusinessHandler implements EventHandler<NettyDisruptorEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DisruptorBusinessHandler.class);
    private volatile long a;

    @Override
    public void onEvent(NettyDisruptorEvent event, long sequence, boolean endOfBatch) throws Exception {
//       businessProcess(event, false);
        businessProcess(event, true);
    }

    private void businessProcess(NettyDisruptorEvent event, boolean slow) {
        long t2 = System.nanoTime();
        LOGGER.info("Back pressure cost {}, eventId:{}, payload:{}", t2 - event.getT1(), event.getId(),
                event.getPayload());
        // some business
        String business = event.getPayload() + "--1 \n";
        if (slow) {
            doSomethingSlow();
        }
        Channel channel = event.getCh();

        channel.eventLoop().execute(() -> {
            ByteBuf cur = channel.alloc().directBuffer();
            ByteBufUtil.writeUtf8(cur, business);
            long t3 = System.nanoTime();
            LOGGER.info("Business cost: {}", t3 - t2);
            LOGGER.info("Server End to End time eventId {}, paylod {} : {}", event.getId(), event.getPayload(),
                    System.nanoTime() - event.getT0());
            channel.writeAndFlush(cur);
        });

    }

    private void doSomethingSlow() {
        long x = a;
        for (int i = 0; i < 1000_000; i++) {
            x ^= i;
        }
        a = x;
    }

}
