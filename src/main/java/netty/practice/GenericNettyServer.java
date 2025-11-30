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
        var elg = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        try {
            var bootstrap = new ServerBootstrap();
            bootstrap.group(elg).channel(NioServerSocketChannel.class).localAddress(port);
            addChildHandlerAndSOoptions(bootstrap);
            var channelfuture = bootstrap.bind().sync();
            channelfuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            elg.shutdownGracefully();
        }
    }

    protected abstract void addChildHandlerAndSOoptions(ServerBootstrap bootstrap);

}
