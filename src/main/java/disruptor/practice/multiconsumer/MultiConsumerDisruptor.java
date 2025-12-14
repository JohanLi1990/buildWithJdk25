package disruptor.practice.multiconsumer;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import disruptor.practice.common.IDisruptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public class MultiConsumerDisruptor implements IDisruptor<MultiConsumerEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiConsumerDisruptor.class);

    static void main() throws InterruptedException {
        MultiConsumerDisruptor cur = new MultiConsumerDisruptor();
        int bufferSize = 1024 * 128;
        Disruptor<MultiConsumerEvent> disruptor = new Disruptor<>(() -> new MultiConsumerEvent(), bufferSize,
                DaemonThreadFactory.INSTANCE,
                ProducerType.SINGLE, new BusySpinWaitStrategy());

        // Validation -> Enrichment ;  Logging ->

        ScheduledExecutorService es = Executors.newSingleThreadScheduledExecutor();

        ValidationHandler valHandler = new ValidationHandler();
        EnrichmentHandler enrichmentHandler = new EnrichmentHandler();
        LoggingHandler logger = new LoggingHandler(es);
        // pipeline A -> B, fanout C
        disruptor.handleEventsWith(valHandler).then(enrichmentHandler);
        disruptor.handleEventsWith(logger);
        disruptor.start();
        // Producer produces 1_000_000 events
        RingBuffer<MultiConsumerEvent> ringBuffer = disruptor.getRingBuffer();
        ByteBuffer bb = ByteBuffer.allocate(16);

        cur.statsLogging(es, valHandler, enrichmentHandler, logger, ringBuffer, 20000);
        for (long l = 0; l < (long) 1e7; l++) {
            bb.putLong(0, l);
            // no need try finally, new patterns in 2025
            ringBuffer.publishEvent(cur::translate, bb);
        }
        LOGGER.info("Finished publishing, cursor={}", ringBuffer.getCursor());
        Thread.sleep(1000 * 30);
        disruptor.shutdown();
        // es shutdown hooked to disruptor shutdown event

    }


    public void translate(MultiConsumerEvent event, long sequence, ByteBuffer buffer) {
        event.setId(buffer.getLong(0));
    }

    private void statsLogging(ScheduledExecutorService es, ValidationHandler valHandler,
                              EnrichmentHandler enrichmentHandler, LoggingHandler logger,
                              RingBuffer<MultiConsumerEvent> ringBuffer, int rate) {

        AtomicLong producerPrev = new AtomicLong(0);
        AtomicLong aSeqPrev = new AtomicLong(0);
        AtomicLong bSeqPrev = new AtomicLong(0);
        AtomicLong cSeqPrev = new AtomicLong(0);

        Runnable cur = () -> {
            long producerSeq = ringBuffer.getCursor();
            long lagA = producerSeq - valHandler.getLastSeq();
            long lagB = producerSeq - enrichmentHandler.getLastSeq();
            long lagC = producerSeq - logger.getLastSeq();
            LOGGER.info("lag-A: {}; lag-B: {}; lag-C: {}", lagA, lagB, lagC);

            long prodNow = ringBuffer.getCursor();
            long aNow = valHandler.getLastSeq();
            long bNow = enrichmentHandler.getLastSeq();
            long cNow = logger.getLastSeq();

            double prodTps = (prodNow - producerPrev.getAndSet(prodNow)) * (1000.0 / rate);
            double aTps    = (aNow    - aSeqPrev.getAndSet(aNow))        * (1000.0 / rate);
            double bTps    = (bNow    - bSeqPrev.getAndSet(bNow))        * (1000.0 / rate);
            double cTps    = (cNow    - cSeqPrev.getAndSet(cNow))        * (1000.0 / rate);

            LOGGER.info("producer-TPS:{}/s; A-TPS:{}/s, B-TPS:{}/s; C-TPS:{}/s", prodTps,
                    aTps,
                    bTps,
                    cTps);

        };
        es.scheduleAtFixedRate(cur, 0, rate, TimeUnit.MILLISECONDS);
    }


}
