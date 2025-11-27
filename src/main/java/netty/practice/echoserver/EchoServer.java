package netty.practice.echoserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class EchoServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(EchoServer.class);

    private final int port;

    public EchoServer(int port) {
        this.port = port;
    }

    static void main() throws InterruptedException {
        int port = 9999;
        var echoServer = new EchoServer(port);
        LOGGER.info("Server is going to start");
        echoServer.start();
        LOGGER.info("Server shutting down");
    }

    private void start() throws InterruptedException {
        var elg = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        try {
            var bootstrap = new ServerBootstrap();
            bootstrap.group(elg)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .childHandler(new ChannelInitializer<SocketChannel>(){
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new EchoServerHandler());
                        }
                    });

            var future = bootstrap.bind().sync();
            LOGGER.info("Server finished starting...");
            future.channel().closeFuture().sync();

        } finally {
            elg.shutdownGracefully().sync();
        }

    }
}
