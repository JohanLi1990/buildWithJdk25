package disruptor.practice.oms.handlers;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import disruptor.practice.common.CompletionSink;
import disruptor.practice.common.ReleaseHook;
import disruptor.practice.oms.model.TaskEvent;
import disruptor.practice.oms.model.TaskObject;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisruptorBusinessHandlerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DisruptorBusinessHandlerTest.class);
    private static class AssertingSink implements CompletionSink {
        LongAdder completed = new LongAdder();
        final ConcurrentHashMap<Integer, LongAdder> perfamilyDone = new ConcurrentHashMap<>();
        volatile long lastProgressNanos = System.nanoTime();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        final ConcurrentHashMap<Integer, AtomicInteger> expected = new ConcurrentHashMap<>();

        @Override
        public void onComplete(TaskObject task, String result, int partitionId) {
            try {
                int exp = expected.computeIfAbsent(task.getCorrelationId(), (_) -> new AtomicInteger(1)).getAndIncrement();
                assertEquals(exp, task.getSeqInFamily(), "family=" + task.getCorrelationId());
                lastProgressNanos = System.nanoTime();
            } catch (Throwable t) {
                failure.compareAndSet(null, t);
            } finally {
                completed.increment();
                perfamilyDone.computeIfAbsent(task.getCorrelationId(), (_) -> new LongAdder()).increment();
                lastProgressNanos = System.nanoTime();
            }
        }
    }



    // Test 1
    @Test
    void deterministicInterleaving() throws InterruptedException {
        AssertingSink sink = new AssertingSink();
        Disruptor<TaskEvent> oneTestDisruptor = createOneTestDisruptor(sink, (f, fs) ->{}, 512);
        try {
            long start = System.nanoTime();
            int loops = 20;
            int perLoop = 10 * 2;

            int expectedTotalSubmited = loops * perLoop;
            for (int i = 0; i < loops; i++) {
                publishInterleavingly(oneTestDisruptor.getRingBuffer(), i);
            }

            awaitCompleted(sink, expectedTotalSubmited, 2200, 1);
            Throwable t = sink.failure.get();
            if (t != null) throw new AssertionError(t);
            LOGGER.info("Completed in {} ms", (sink.lastProgressNanos- start) / 1_000_000);
        } finally {
            oneTestDisruptor.shutdown();

        }

    }

    // Test 2
    @Test
    void testHotFamily() throws InterruptedException {
        AssertingSink assertingSink = new AssertingSink();
        Disruptor<TaskEvent> testTwoDisruptor = createOneTestDisruptor(assertingSink, (_, _) -> {}, 1 << 15);
        try {
            long start = System.nanoTime();
            RingBuffer<TaskEvent> ringBuffer = testTwoDisruptor.getRingBuffer();
            int expectedTotalSubmmited = 12000;
            for (int i = 0; i < 1000; i++) {
                long t0 = System.nanoTime();
                TaskEvent taskA = new TaskEvent(t0, i,i + 1, 1, "task A-" + i, null);
                TaskEvent taskB = new TaskEvent(t0, i + 100, i + 1, 2, "task B-" + i, null);
                assertTrue(ringBuffer.tryPublishEvent(NettyIOHandler::translate, taskA));
                assertTrue(ringBuffer.tryPublishEvent(NettyIOHandler::translate, taskB));
            }

            for (int i = 0; i < 10000; i++) {
                long t0 = System.nanoTime();
                TaskEvent taskC = new TaskEvent(t0, i, i + 1, 999, "task C-"+i, null);
                assertTrue(ringBuffer.tryPublishEvent(NettyIOHandler::translate, taskC));
            }

            awaitFamilyDone(assertingSink, 1, 1000, 17_000, 100);
            awaitFamilyDone(assertingSink, 2, 1000, 17_000, 100);
            LOGGER.info("Completed task A, B in {} ms", (System.nanoTime() - start) / 1_000_000);

            awaitCompleted(assertingSink, expectedTotalSubmmited, 120_000, 1_000, (k) -> LOGGER.info("{} completed so" +
                    " far", k));
            Throwable t = assertingSink.failure.get();
            if (t != null) throw new AssertionError(t);

            LOGGER.info("Completed All in {} ms", (System.nanoTime() - start) / 1_000_000);
        } finally {
            testTwoDisruptor.shutdown();

        }
    }

    // Test 3
    @Test
    void testRaceHammer() throws InterruptedException {
        AssertingSink sink = new AssertingSink();

        /**
         * just used for test 3
         */
        final ConcurrentHashMap<Integer, AtomicInteger> sequenceGenerator = new ConcurrentHashMap<>();
        final int family = 999;
        final int initial = 1;
        final int injections = 2_000; // around 20s.
        /**
         * add one task to familyState.pending at the last minute
         */
        sequenceGenerator.put(family, new AtomicInteger(initial));
        AtomicInteger left = new AtomicInteger(injections);
        final ReleaseHook testReleaseHook = (fam, familyState) -> {
            if (fam != family) return;
            int k = left.getAndDecrement();
            if (k <= 0) return;
            int seq = sequenceGenerator.get(family).incrementAndGet();
            TaskObject to = new TaskObject(-1, family, seq, null, false,
                    "Injected@"+seq, 0L);
            familyState.getPending().offer(to);
        };

        Disruptor<TaskEvent> testThreeDisruptor = createOneTestDisruptor(sink, testReleaseHook, 1024);
        try {
            //
            RingBuffer<TaskEvent> rb = testThreeDisruptor.getRingBuffer();
            long t0 = System.nanoTime();
            TaskEvent seed = new TaskEvent(t0, 1, 1, family, "seed", null);
            assertTrue(rb.tryPublishEvent(NettyIOHandler::translate, seed));
            int expectedTotal = initial + injections;
            awaitCompleted(sink, expectedTotal, 60_000, 50);
            Throwable throwable = sink.failure.get();
            if (throwable != null) {
                throw new AssertionError(throwable);
            }
            assertTrue(left.get() <= 0, "did not inject all tasks; left="+left.get());
            LOGGER.info("Completed all in {} ms", (System.nanoTime() - t0) / 1_000_000);
        } finally {
            testThreeDisruptor.shutdown();
        }

    }


    private void awaitFamilyDone(AssertingSink sink, int familyId, int expected, int timeoutMs, int interval) throws InterruptedException {
        LongAdder counter = sink.perfamilyDone.computeIfAbsent(familyId, _ -> new LongAdder());
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        while (counter.sum() < expected && System.nanoTime() < deadline) {
            Thread.sleep(interval);
            Throwable t = sink.failure.get();
            if (t != null) throw new AssertionError(t);
        }
        assertEquals(expected, counter.sum(),
                "timeout: completed=" + sink.perfamilyDone.get(familyId).sum() + " expected=" + expected);
    }

    private void awaitCompleted(AssertingSink sink, int expected, int timeoutMs, int interval) throws InterruptedException {
        awaitCompleted(sink, expected, timeoutMs, interval, null);
    }

    private void awaitCompleted(AssertingSink sink, int expected, int timeoutMs, int interval,
                                Consumer<Long> logger) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        while (sink.completed.sum() < expected && System.nanoTime() < deadline) {
            if (logger != null) {
                logger.accept(sink.completed.sum());
            }
            Throwable t = sink.failure.get();
            if (t != null) throw new AssertionError(t);
            Thread.sleep(interval);
        }
        assertEquals(expected, sink.completed.sum(),
                "timeout: completed=" + sink.completed.sum() + " expected=" + expected);
    }

    private void publishInterleavingly(RingBuffer<TaskEvent> ringBuffer, int round) {
        // A(1..10), B(1..10)
        for (int i = 0; i < 10; i++) {
            long t0 = System.nanoTime();
            TaskEvent taskA = new TaskEvent(t0, i, 10 * round + i + 1, 1, "task A-" + i, null);
            TaskEvent taskB = new TaskEvent(t0, i + 100, 10 * round + i + 1, 2, "task B-" + i, null);
            // all publish should succeed
            assertTrue(ringBuffer.tryPublishEvent(NettyIOHandler::translate, taskA));
            assertTrue(ringBuffer.tryPublishEvent(NettyIOHandler::translate, taskB));
        }
    }

    private Disruptor<TaskEvent> createOneTestDisruptor(AssertingSink sink, ReleaseHook hook, int ringSize) {
        Disruptor<TaskEvent> cur = new Disruptor<>(TaskEvent::new, ringSize, DaemonThreadFactory.INSTANCE,
                ProducerType.MULTI, new BlockingWaitStrategy());
        ThreadPoolExecutor testWorkerPool = createTestDisruptorWorkerPool();
        cur.handleEventsWith(new DisruptorBusinessHandler(0, testWorkerPool, sink, hook));
        cur.start();
        return cur;
    }

    private ThreadPoolExecutor createTestDisruptorWorkerPool() {
        ThreadFactory threadFactory = r -> new Thread(r, "TestWorker");
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(4, 4, 10, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(512), threadFactory);
        threadPoolExecutor.setRejectedExecutionHandler((r, exec) -> {
            LOGGER.error("TPE REJECTED task. poolSize={} active={} completed={} taskCount={}",
                    exec.getPoolSize(), exec.getActiveCount(), exec.getCompletedTaskCount(), exec.getTaskCount());
            throw new RejectedExecutionException("rejected");
        });
        return threadPoolExecutor;
    }

}