package disruptor.practice.multiconsumer;

import com.lmax.disruptor.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Consumer A: Validation
 *
 * Reads event fields and sets a flag like valid=true/false
 *
 * Or sets a rejectReason if invalid
 * (Don’t throw exceptions yet; handle invalid in-band)
 */
public class ValidationHandler implements EventHandler<MultiConsumerEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationHandler.class);
    private volatile long lastSeq;

    @Override
    public void onEvent(MultiConsumerEvent event, long sequence, boolean endOfBatch) throws Exception {
        lastSeq = sequence;
        event.setValid((event.getId() & 1L) == 0);

//        perEventLogging(event, sequence, 100_000);
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


}
