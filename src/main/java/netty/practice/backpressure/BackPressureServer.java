package netty.practice.backpressure;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import netty.practice.GenericNettyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BackPressureServer extends GenericNettyServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackPressureServer.class);
    private final ExecutorService businessThreadpool = Executors.newFixedThreadPool(12);
    public BackPressureServer(int port) {
        super(port);
    }

    @Override
    public void start() {
        var bossGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        var workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        try {
            var bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup);
            bootstrap.localAddress(port);
            bootstrap.channel(NioServerSocketChannel.class);
            addChildHandlerAndSOoptions(bootstrap);

            var cf = bootstrap.bind().sync();
            cf.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            this.businessThreadpool.shutdown();
        }
    }

    @Override
    protected void addChildHandlerAndSOoptions(ServerBootstrap bootstrap) {
        bootstrap.childOption(ChannelOption.AUTO_READ, false);
//        bootstrap.childOption(ChannelOption.AUTO_READ, true);
        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new LineBasedFrameDecoder(1024));
                ch.pipeline().addLast(new ServerReadLoggingHandler());
                ch.pipeline().addLast(new ServerSlowBusinessHandler(businessThreadpool));
            }
        });

    }

    static void main() {
        LOGGER.info("Backpressure server starting...");
        new BackPressureServer(9999).start();
        LOGGER.info("Backpressure server shutting down...");
    }

}
