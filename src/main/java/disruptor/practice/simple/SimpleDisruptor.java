package disruptor.practice.simple;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SingleProducerSequencer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.info.GraphLayout;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class SimpleDisruptor {


    static void main() throws InterruptedException {
        int bufferSize = 1024 * 64;
        LongEventFactory eventFactory = new LongEventFactory();
        ThreadFactory cur = Executors.defaultThreadFactory();
        CountDownLatch c1 = new CountDownLatch(1);

        Disruptor<LongEvent> disruptor = new Disruptor<>(eventFactory, bufferSize, cur, ProducerType.SINGLE,
                new BlockingWaitStrategy());

        disruptor.handleEventsWith(new SimpleEventHandler(c1, 1_000_000));
//        System.out.println(ClassLayout.parseClass(SingleProducerSequencer.class).toPrintable());
        disruptor.start();

        RingBuffer<LongEvent> ringBuffer = disruptor.getRingBuffer();
        ByteBuffer bb = ByteBuffer.allocate(1024);

        long startTime = System.nanoTime();
        System.out.println("Current timing: " + startTime);
        for (long l = 0; l < (long)1e6; l++) {
            bb.putLong(0, l);
            // no need try finally, new patterns in 2025
            ringBuffer.publishEvent(SimpleDisruptor::translate, bb);
        }
        c1.await();
        System.out.println("TimeElapsed: " + (System.nanoTime() - startTime));
        disruptor.shutdown();
    }

    static void translate(LongEvent event, long sequence, ByteBuffer buffer) {
        event.setId(buffer.getLong(0));
    }
}
