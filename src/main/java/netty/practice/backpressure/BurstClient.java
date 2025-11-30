package netty.practice.backpressure;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import netty.practice.GenericNettyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BurstClient extends GenericNettyClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(BurstClient.class);
    public BurstClient(String host, int port) {
        super(host, port);
    }

    @Override
    protected void addHandlersAndSOOptions(Bootstrap bootstrap) {
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
//                ch.pipeline().addLast(new LineBasedFrameDecoder(1024));
                ch.pipeline().addLast(new BurstClientHandler());
            }
        });
    }

    static void main() {
        LOGGER.info("Burst Client starting...");
        new BurstClient("127.0.0.1", 9999).start();
        LOGGER.info("Burst Client shutting down...");
    }
}
