package netty.practice;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public abstract class GenericNettyServer {
    protected final int port;

    public GenericNettyServer(int port) {
        this.port = port;
    }

    public void start() {
        var bossGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        var workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        try {
            var bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).localAddress(port);
            addChildHandlerAndSOoptions(bootstrap);
            var channelfuture = bootstrap.bind().sync();
            channelfuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            additionalShutdownSequence();
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    protected void additionalShutdownSequence() {}

    protected abstract void addChildHandlerAndSOoptions(ServerBootstrap bootstrap);

}
