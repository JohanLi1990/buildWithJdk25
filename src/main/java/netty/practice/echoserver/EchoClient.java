package netty.practice.echoserver;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;

public class EchoClient {

    int port;
    final String host;

    public EchoClient(int port, String host) {
        this.port = port;
        this.host = host;
    }

    static void main() {
        new EchoClient(9999, "127.0.0.1").start();
    }

    public void start() {
        var elg = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        try {
            var bootstrap = new Bootstrap();
            bootstrap.group(elg)
                    .channel(NioSocketChannel.class)
                    .remoteAddress(new InetSocketAddress(host, port))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new EchoClientHandler());
                        }
                    });
            var channelFuture = bootstrap.connect().sync();
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            elg.shutdownGracefully();
        }
    }
}
