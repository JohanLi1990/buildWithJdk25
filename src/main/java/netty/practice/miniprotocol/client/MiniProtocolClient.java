package netty.practice.miniprotocol.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import netty.practice.GenericNettyClient;
import netty.practice.miniprotocol.codec.FixMessageDecoder;
import netty.practice.miniprotocol.server.MiniProtocolServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MiniProtocolClient extends GenericNettyClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(MiniProtocolClient.class);
    public MiniProtocolClient(String host, int port) {
        super(host, port);
    }

    @Override
    protected void addHandlersAndSOOptions(Bootstrap bootstrap) {
        bootstrap.channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
                ch.pipeline().addLast(new LineBasedFrameDecoder(1024));
                ch.pipeline().addLast(new FixMessageDecoder());
                ch.pipeline().addLast(new ClientHandler());
            }
        });
    }

    static void main() {
        LOGGER.info("MiniProtocolClient starting...");
        new MiniProtocolClient("127.0.0.1", 9999).start();
        LOGGER.info("MiniProtocolClient closing...");
    }
}
