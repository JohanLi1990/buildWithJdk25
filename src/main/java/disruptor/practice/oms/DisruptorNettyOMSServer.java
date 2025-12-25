package disruptor.practice.oms;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import disruptor.practice.oms.handlers.*;
import disruptor.practice.oms.handlers.codec.NettyOrderEventDecoder;
import disruptor.practice.oms.handlers.codec.NettyOrderEventEncoder;
import disruptor.practice.oms.model.TaskEvent;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import netty.practice.GenericNettyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DisruptorNettyOMSServer extends GenericNettyServer {
    private static final Logger log = LoggerFactory.getLogger(DisruptorNettyOMSServer.class);
    private final List<Disruptor<TaskEvent>> disruptors;

    public DisruptorNettyOMSServer(int port) {
        super(port);
        disruptors = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            disruptors.add(initDisruptor(i));
        }
    }

    private static Disruptor<TaskEvent> initDisruptor(int partitionId) {
        Disruptor<TaskEvent> cur =  new Disruptor<>(TaskEvent::new, 1024, DaemonThreadFactory.INSTANCE,
                ProducerType.MULTI, /* Have to use multi here, because mutltiple client*/
                new BlockingWaitStrategy());
        cur.handleEventsWith(new DisruptorBusinessHandler(partitionId));
        cur.start();
        return cur;
    }

    @Override
    protected void addChildHandlerAndSOoptions(ServerBootstrap bootstrap) {
         bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {

             @Override
             protected void initChannel(SocketChannel ch) throws Exception {
                 // still need a decoder
                 ch.pipeline().addLast(new LineBasedFrameDecoder(1024));
                 ch.pipeline().addLast(new NettyOrderEventDecoder());
                 ch.pipeline().addLast(new NettyOrderEventEncoder());
                 ch.pipeline().addLast(new NettyIOHandler(disruptors));
             }
         });
    }

    static void main() {
        log.info("DisruptorNettyOMSServer starting....");
        new DisruptorNettyOMSServer(9999).start();
        log.info("Server stopped....");
    }
}
