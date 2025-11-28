package netty.practice.pipelineserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import netty.practice.GenericNettyServer;
import netty.practice.pipelineserver.serverhandlers.inbound.ServerBusinessHandler;
import netty.practice.pipelineserver.serverhandlers.inbound.ServerHeadLogger;
import netty.practice.pipelineserver.serverhandlers.inbound.ServerTaiLogger;
import netty.practice.pipelineserver.serverhandlers.inbound.ServerTransformHandler;
import netty.practice.pipelineserver.serverhandlers.outbound.BufferingHandler;
import netty.practice.pipelineserver.serverhandlers.outbound.ModifierHandler;
import netty.practice.pipelineserver.serverhandlers.outbound.TailOutboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PipelineServer extends GenericNettyServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineServer.class);

    public PipelineServer(int port) {
        super(port);
    }

    @Override
    protected void addChildHandlerAndSOoptions(ServerBootstrap bootstrap) {
        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ch.pipeline().addLast("in1", new ServerHeadLogger())
                        .addLast("in2", new ServerTransformHandler())
                        .addLast("in3", new ServerBusinessHandler())
                        .addLast("in4", new ServerTaiLogger())
                        .addLast("out1", new TailOutboundHandler())
                        .addLast("out2", new ModifierHandler())
                        .addLast("out3", new BufferingHandler());
            }
        });
    }


    static void main() {
        LOGGER.info("server starting....");
        new PipelineServer(9999).start();
        LOGGER.info("server shutting down.... ");
    }


}
