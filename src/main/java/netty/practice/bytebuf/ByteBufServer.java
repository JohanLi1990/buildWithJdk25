package netty.practice.bytebuf;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import netty.practice.GenericNettyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ByteBufServer extends GenericNettyServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ByteBufServer.class);
    public ByteBufServer(int port) {
        super(port);
    }

    @Override
    protected void addChildHandlerAndSOoptions(ServerBootstrap bootstrap) {
        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new ByteBufHandler()).addLast(new ByteBufHandler2());

            }
        });
    }

    static void main() {
        var byteBufServer = new ByteBufServer(9999);
        LOGGER.info("{} starting....", byteBufServer.getClass().getName());
        byteBufServer.start();
        LOGGER.info("{} shutting down...", byteBufServer.getClass().getName());
    }


}
