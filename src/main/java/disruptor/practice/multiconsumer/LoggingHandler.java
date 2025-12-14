package disruptor.practice.multiconsumer;

import com.lmax.disruptor.EventHandler;
import disruptor.practice.common.IPerEventLogging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

public class LoggingHandler implements EventHandler<MultiConsumerEvent>, IPerEventLogging<MultiConsumerEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingHandler.class);
    private long starTime;
    private volatile long lastSeq;
    private final ScheduledExecutorService es;

    public LoggingHandler(ScheduledExecutorService es) {
        this.es = es;
    }

    @Override
    public void onEvent(MultiConsumerEvent event, long sequence, boolean endOfBatch) throws Exception {
//        perEventLogging(event, sequence, 100_000);
        lastSeq = sequence;
    }

    public long getLastSeq() {
        return lastSeq;
    }

    public void perEventLogging(MultiConsumerEvent event, long sequence , int rate) {
        if (event.getId() % rate == 0) {
            LOGGER.info("sequence {}; eventId={}; isValid? {}; enrichment: {}", sequence, event.getId(), event.isValid(),
                    event.getEnrichedValue());
//            Thread.sleep(1000);
        }
    }

    @Override
    public void onShutdown() {
        LOGGER.info("Disruptor shutting down... Time elapsed:{}ns", System.nanoTime() - starTime);
        es.shutdown();
    }


    @Override
    public void onStart() {
        System.out.println("Start!!...");
        this.starTime = System.nanoTime();
    }
}
