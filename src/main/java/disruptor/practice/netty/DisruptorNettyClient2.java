package disruptor.practice.netty;

import disruptor.practice.netty.handlers.ClientInboundHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import netty.practice.GenericNettyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DisruptorNettyClient2 extends GenericNettyClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(DisruptorNettyClient2.class);
    public DisruptorNettyClient2(String host, int port) {
        super(host, port);
    }

    @Override
    protected void addHandlersAndSOOptions(Bootstrap bootstrap) {
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new LineBasedFrameDecoder(1024));
                ch.pipeline().addLast(new ClientInboundHandler(false));
            }
        });
    }

    static void main() {
        LOGGER.info("Client starts...");
        new DisruptorNettyClient2("127.0.0.1", 9999).start();
        LOGGER.info("Client stops...");
    }
}
