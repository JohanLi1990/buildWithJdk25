package disruptor.practice.simple;

import com.lmax.disruptor.EventHandler;

import java.util.concurrent.CountDownLatch;

public class SimpleEventHandler implements EventHandler<LongEvent> {
//    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleEventHandler.class);
    private long processedCount = 0L;
    private final long targetCount;
    private final CountDownLatch cdl;

    public SimpleEventHandler(CountDownLatch c1, int i) {
        this.targetCount = i;
        this.cdl = c1;
    }

    @Override
    public void onEvent(LongEvent longEvent, long l, boolean b)  {
        // It is very costly to do logging or System.out.println(...) here
//        long cur = longEvent.getId();
//        LOGGER.info("reading event id {}", cur);
//        System.out.println(Thread.currentThread().getName() + ": Read Id " + cur);
        processedCount++;
        if (processedCount == targetCount) {
            cdl.countDown();
        }
    }

    @Override
    public void onShutdown() {
        System.out.println("Total Read: " + processedCount);
    }
}
