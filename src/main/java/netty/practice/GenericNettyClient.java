package netty.practice;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GenericNettyClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenericNettyClient.class);
    private final String host;
    private final int port;

    public GenericNettyClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        var elg = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        try {
            var bootstrap = new Bootstrap();
            bootstrap.group(elg)
                    .remoteAddress(host, port).channel(NioSocketChannel.class);
            addHandlersAndSOOptions(bootstrap);
            var channelFuture = bootstrap.connect().sync();
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            LOGGER.error("Exceptions encounter...{}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            elg.shutdownGracefully();
        }
    }

    protected abstract void addHandlersAndSOOptions(Bootstrap bootstrap);


}
