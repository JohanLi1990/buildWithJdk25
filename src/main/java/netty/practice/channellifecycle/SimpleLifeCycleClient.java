package netty.practice.channellifecycle;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import netty.practice.GenericNettyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleLifeCycleClient extends GenericNettyClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleLifeCycleClient.class);

    public SimpleLifeCycleClient(String host, int port) {
        super(host, port);
    }

    static void main() {
        LOGGER.info("Client Starts....");
        new SimpleLifeCycleClient("127.0.0.1", 9999).start();
        LOGGER.info("Client ends...");
    }

    @Override
    protected void addHandlersAndSOOptions(Bootstrap bootstrap) {
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
                ch.pipeline().addLast(new SimpleClientSend());
            }
        });
    }
}
