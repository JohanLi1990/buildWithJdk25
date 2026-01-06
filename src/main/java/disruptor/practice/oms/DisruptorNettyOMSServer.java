package disruptor.practice.oms;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import disruptor.practice.common.CompletionSink;
import disruptor.practice.oms.handlers.DisruptorBusinessHandler;
import disruptor.practice.oms.handlers.NettyIOHandler;
import disruptor.practice.oms.handlers.codec.NettyOrderEventDecoder;
import disruptor.practice.oms.handlers.codec.NettyOrderEventEncoder;
import disruptor.practice.oms.model.PartitionMetrics;
import disruptor.practice.oms.model.TaskEvent;
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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DisruptorNettyOMSServer extends GenericNettyServer {
    private static final Logger log = LoggerFactory.getLogger(DisruptorNettyOMSServer.class);
    private final List<Disruptor<TaskEvent>> disruptors;
    private final int N = 8;
    private final CompletionSink nettySink = (t, res) -> {
        Channel channel = t.getChannel();
        channel.eventLoop().execute(() -> {
            channel.write(res);
            if (t.isEob()) {
                channel.flush();
            }
        });
    };

    private final ConcurrentHashMap<Integer, DisruptorBusinessHandler> handlers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService statsScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "stat-logger");
        thread.setDaemon(true);
        return thread;
    });

    // per-partition previous snapshot
    private final ConcurrentHashMap<Integer, PartitionMetrics.PartitionMetricSnapshot> prev = new ConcurrentHashMap<>();

    public DisruptorNettyOMSServer(int port) {
        super(port);
        disruptors = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            disruptors.add(initDisruptor(i));
        }
    }

    private Disruptor<TaskEvent> initDisruptor(int partitionId) {
        Disruptor<TaskEvent> cur = new Disruptor<>(TaskEvent::new, 64, DaemonThreadFactory.INSTANCE,
                ProducerType.MULTI, /* Have to use multi here, because mutltiple client*/
                new BlockingWaitStrategy());
        var es = createDisruptorWorkerPool(partitionId);
        // inject a dummy release hook here;
        // release hook is mostly used in tests
        DisruptorBusinessHandler handler = new DisruptorBusinessHandler(partitionId, es, nettySink, (_, _) -> {
        });
        cur.handleEventsWith(handler);
        handlers.put(partitionId, handler);
        cur.start();

        // Start periodic Loggin for this partition
        schedulePartitionLogging(partitionId);
        return cur;
    }

    private void schedulePartitionLogging(int partitionId) {
        // Log every 1 second
        statsScheduler.scheduleAtFixedRate(() -> {
            DisruptorBusinessHandler h = handlers.get(partitionId);
            if (h == null) return;
            long nowNs = System.nanoTime();
            PartitionMetrics.PartitionMetricSnapshot curSnap = h.snapshot(nowNs);
            PartitionMetrics.PartitionMetricSnapshot prevSnap = prev.put(partitionId, curSnap);
            // First tick no delta yet
            if (prevSnap == null) return;
            long dtNs = curSnap.nanoNs() - prevSnap.nanoNs();
            if (dtNs <= 0) return;
            double dtSec  = dtNs / 1_000_000_000.0;
            long inDelta = curSnap.taskIn() - prevSnap.taskIn();
            long dispDelta = curSnap.dispatched() - prevSnap.dispatched();
            long doneDelta = curSnap.completed() - prevSnap.completed();
            long enqDelta = curSnap.enqueuedDueToBusy() - prevSnap.enqueuedDueToBusy();

            long inRate = (long) (inDelta / dtSec);
            long dispRate = (long) (dispDelta / dtSec);
            long doneRate = (long) (doneDelta / dtSec);
            long enqRate = (long) (enqDelta / dtSec);

            DisruptorBusinessHandler.PendingStats pendingStats = null;
            int t = h.tick();
            if ((t % 5) == 0) {
                pendingStats = h.samplePending();
            }
            if (pendingStats == null) {


                log.info("P{} in={} (+{}/s) disp={} (+{}/s) done={} (+{}/s) enqBusy={} (+{}/s) inFlightFam={} " +
                                "maxPendObs={} tpe(pool={} active={} q={})",
                        partitionId,
                        curSnap.taskIn(), inRate,
                        curSnap.dispatched(), dispRate,
                        curSnap.completed(), doneRate,
                        curSnap.enqueuedDueToBusy(), enqRate,
                        curSnap.inFlightFamilies(),
                        curSnap.maxObserved(),
                        curSnap.poolSize(), curSnap.activeThreads(), curSnap.qSize()
                );
            } else {
                log.info(
                        "P{} in={} (+{}/s) disp={} (+{}/s) done={} (+{}/s) enqBusy={} (+{}/s) inFlightFam={} " +
                                "maxPendObs={} famSeen={} famPend={} pendTot={} pendMaxNow={} " +
                                "tpe(pool={} active={} q={})",
                        partitionId,
                        curSnap.taskIn(), inRate,
                        curSnap.dispatched(), dispRate,
                        curSnap.completed(), doneRate,
                        curSnap.enqueuedDueToBusy(), enqRate,
                        curSnap.inFlightFamilies(),
                        curSnap.maxObserved(),
                        pendingStats.familiesSeen(),
                        pendingStats.familiesWithPending(),
                        pendingStats.pendingTotal(),
                        pendingStats.pendingMaxNow(),
                        curSnap.poolSize(), curSnap.activeThreads(), curSnap.qSize()
                );
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private ThreadPoolExecutor createDisruptorWorkerPool(int partitionId) {
        ThreadFactory tFactory = r -> new Thread(r, "TW-" + partitionId);
        var es = new ThreadPoolExecutor(1, 8, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(8), tFactory);
        es.setRejectedExecutionHandler((r, exec) -> {
            log.error("TPE REJECTED task. partition={} poolSize={} active={} completed={} taskCount={}",
                    partitionId, exec.getPoolSize(), exec.getActiveCount(), exec.getCompletedTaskCount(), exec.getTaskCount());
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

    // shutdown sequence when Netty Server is terminated
    @Override
    protected void additionalShutdownSequence() {
        super.additionalShutdownSequence();
        this.statsScheduler.shutdownNow();
    }

    static void main() {
        log.info("DisruptorNettyOMSServer starting....");
        new DisruptorNettyOMSServer(9999).start();
        log.info("Server stopped....");
    }
}
