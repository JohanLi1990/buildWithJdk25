package disruptor.practice.netty;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import disruptor.practice.netty.events.NettyDisruptorEvent;
import disruptor.practice.netty.handlers.DisruptorBusinessHandler;
import disruptor.practice.netty.handlers.ServerNettyDisruptorHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import netty.practice.GenericNettyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DisruptorNettyTcpServer extends GenericNettyServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DisruptorNettyTcpServer.class);
    private final Disruptor<NettyDisruptorEvent> disruptor;

    public DisruptorNettyTcpServer(int port) {
        super(port);
        this.disruptor = initDisruptors();
    }

    private Disruptor<NettyDisruptorEvent> initDisruptors() {
        Disruptor<NettyDisruptorEvent> cur = new Disruptor<>(NettyDisruptorEvent::new, 1024,
                DaemonThreadFactory.INSTANCE, ProducerType.SINGLE, new BlockingWaitStrategy());
        cur.handleEventsWith(new DisruptorBusinessHandler());
        // start disruptor once!
        cur.start();
        return cur;
    }

    @Override
    protected void addChildHandlerAndSOoptions(ServerBootstrap bootstrap) {
        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {

                // day 3
//                ch.pipeline().addLast(new ServerInboundEchoHandler());
//                ch.pipeline().addLast(new ServerInboundExtraHandler());

                // day 4
                ch.pipeline().addLast(new LineBasedFrameDecoder(1024));
                ch.pipeline().addLast(new ServerNettyDisruptorHandler(disruptor));

            }
        });
    }

    static void main() {
        LOGGER.info("Server starts...");
        new DisruptorNettyTcpServer(9999).start();
        LOGGER.info("Server stopped!...");
    }
}
