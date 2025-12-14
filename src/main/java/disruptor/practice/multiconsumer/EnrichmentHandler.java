package disruptor.practice.multiconsumer;

import com.lmax.disruptor.EventHandler;
import disruptor.practice.common.IPerEventLogging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnrichmentHandler implements EventHandler<MultiConsumerEvent>, IPerEventLogging<MultiConsumerEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnrichmentHandler.class);
    private volatile long lastSeq;
    private long sink;

    @Override
    public void onEvent(MultiConsumerEvent event, long sequence, boolean endOfBatch) throws Exception {
        lastSeq = sequence;
        long id = event.getId();
        if (event.isValid()) {
            event.setEnrichedValue(id + 100L);
        }
        long x = sink;
        for (int i = 0 ; i < 50_000; i++) {
            x ^= i;
        }
        sink = x;
        // perEventLogging(event, sequence, 100_000);
    }

    public long getLastSeq() {
        return lastSeq;
    }

    @Override
    public void perEventLogging(MultiConsumerEvent event, long sequence , int rate) {
        if (event.getId() % rate == 0) {
            LOGGER.info("sequence {}; eventId={}; isValid? {}; enrichment: {}", sequence, event.getId(), event.isValid(),
                    event.getEnrichedValue());
//            Thread.sleep(1000);
        }
    }
}
