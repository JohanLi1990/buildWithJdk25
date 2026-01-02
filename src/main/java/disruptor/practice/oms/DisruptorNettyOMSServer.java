package disruptor.practice.oms;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import disruptor.practice.common.CompletionSink;
import disruptor.practice.oms.handlers.*;
import disruptor.practice.oms.handlers.codec.NettyOrderEventDecoder;
import disruptor.practice.oms.handlers.codec.NettyOrderEventEncoder;
import disruptor.practice.oms.model.TaskEvent;
import disruptor.practice.oms.model.TaskResponse;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import netty.practice.GenericNettyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class DisruptorNettyOMSServer extends GenericNettyServer {
    private static final Logger log = LoggerFactory.getLogger(DisruptorNettyOMSServer.class);
    private final List<Disruptor<TaskEvent>> disruptors;
    private final int N = 8;
    private final CompletionSink nettySink = (taskObject, res, partitionId) -> {
        // asynchronously write back
        Channel channel = taskObject.getChannel();
        channel.eventLoop().execute(() -> {
            TaskResponse msg = new TaskResponse(taskObject.getOrderId(),
                    taskObject.getCorrelationId(),
                    partitionId, taskObject.getSeqInFamily(), res, taskObject.getT0(), System.nanoTime(), taskObject.getPayload());
            if (taskObject.isEob()) {
                channel.writeAndFlush(msg);
            } else {
                channel.write(msg);
            }
        });
    };

    public DisruptorNettyOMSServer(int port) {
        super(port);
        disruptors = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            disruptors.add(initDisruptor(i));
        }
    }

    private Disruptor<TaskEvent> initDisruptor(int partitionId) {
        Disruptor<TaskEvent> cur =  new Disruptor<>(TaskEvent::new, 2048, DaemonThreadFactory.INSTANCE,
                ProducerType.MULTI, /* Have to use multi here, because mutltiple client*/
                new BlockingWaitStrategy());
        var es = createDisruptorWorkerPool(partitionId);
        // inject a dummy release hook here;
        // release hook is mostly used in tests
        cur.handleEventsWith(new DisruptorBusinessHandler(partitionId, es, nettySink, (_, _) -> {}));
        cur.start();
        return cur;
    }

    private ThreadPoolExecutor createDisruptorWorkerPool(int partitionId) {
        ThreadFactory tFactory = r -> new Thread(r, "TW-"+ partitionId);
        var es = new ThreadPoolExecutor(2, 8, 10, TimeUnit.SECONDS, new SynchronousQueue<>(false), tFactory);
        es.setRejectedExecutionHandler((r, exec) -> {
            log.error("TPE REJECTED task. poolSize={} active={} completed={} taskCount={}",
                    exec.getPoolSize(), exec.getActiveCount(), exec.getCompletedTaskCount(), exec.getTaskCount());
            throw new RejectedExecutionException("rejected");
        });
        return es;
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
