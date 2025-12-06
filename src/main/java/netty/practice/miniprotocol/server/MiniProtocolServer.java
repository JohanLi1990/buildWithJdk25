package netty.practice.miniprotocol.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import netty.practice.GenericNettyServer;
import netty.practice.miniprotocol.codec.FixMessageDecoder;
import netty.practice.miniprotocol.codec.FixMessageEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MiniProtocolServer extends GenericNettyServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MiniProtocolServer.class);

    public MiniProtocolServer(int port) {
        super(port);
    }

    @Override
    protected void addChildHandlerAndSOoptions(ServerBootstrap bootstrap) {
        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new LineBasedFrameDecoder(1024));
                ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
                ch.pipeline().addLast(new FixMessageDecoder());
                ch.pipeline().addLast(new FixMessageEncoder());
                ch.pipeline().addLast(new BusinessHandler());
            }
        });

    }

    static void main() {
        LOGGER.info("MiniProtocolServer starting...");
        new MiniProtocolServer(9999).start();
        LOGGER.info("MiniProtocolServer shutting down...");
    }
}
