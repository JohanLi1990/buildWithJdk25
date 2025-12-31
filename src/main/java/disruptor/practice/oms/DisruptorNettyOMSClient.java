package disruptor.practice.oms;

import disruptor.practice.oms.handlers.NettyClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import netty.practice.GenericNettyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DisruptorNettyOMSClient extends GenericNettyClient {

    private static final Logger log = LoggerFactory.getLogger(DisruptorNettyOMSClient.class);

    public DisruptorNettyOMSClient(String host, int port) {
        super(host, port);
    }

    @Override
    protected void addHandlersAndSOOptions(Bootstrap bootstrap) {
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new LineBasedFrameDecoder(1024))
                        .addLast(new NettyClientHandler());
            }
        });
    }

    static void main() {
        log.info("Client starts...");
        new DisruptorNettyOMSClient("127.0.0.1", 9999).start();
        log.info("Client stops...");
    }


}
