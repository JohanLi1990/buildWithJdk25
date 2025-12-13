package netty.practice.channellifecycle;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import netty.practice.GenericNettyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleLifeCycleServer extends GenericNettyServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleLifeCycleServer.class);
    public SimpleLifeCycleServer(int port) {
        super(port);
    }

    static void main() {
        LOGGER.info("Starting Netty Day 6...");
        new SimpleLifeCycleServer(9999).start();
        LOGGER.info("Ending Netty day 6...");
    }

    @Override
    protected void addChildHandlerAndSOoptions(ServerBootstrap bootstrap) {
        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new LifeCycleLoggingHandler());
                ch.pipeline().addLast(new SimpleChannelWriteHandler());
                ch.pipeline().addLast(new SimpleMessageHandler());
            }
        });
    }
}

// Scenario A:Abrupt Client Close (Disconnects)
/*
when clients kill connection:
- java.net.SocketException: Connection reset
- then channelInactive
- then channelUnregister, handlerRemoved

when server closes:
- channelInactive - INFO  channelInactive...
- channelUnregistered....
- handlerRemoved - INFO  handlerRemoved triggered....
No exception caught
 */

// Scenario B:
/*
Case	                    Decoder throws?	    exceptionCaught?	    Channel closed? Who closes?
Too long frame	            Yes/No	            ?	                    ?
Invalid format	            Yes/No	            ?	                    ?
Never completes a frame	    No	                ?   	                ?

 */

// Scenario C:
/*
- exceptionCaught is fired in DefaultChannelPipeline, onUnhandleInboundException
- channle actually remain usable
 */