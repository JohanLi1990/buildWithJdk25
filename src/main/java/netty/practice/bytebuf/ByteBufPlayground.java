package netty.practice.bytebuf;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ByteBufPlayground {
    private static final Logger LOGGER = LoggerFactory.getLogger(ByteBufPlayground.class);
    static void main() {
        var heapBuf = new ByteBufPlayground();
        heapBuf.practice1();
    }


    void practice1() {
        /*
        Allocate two buffers:

        One heap buffer.

        One direct buffer.

        One using pooled allocator, one using Unpooled (so you see both worlds).
         */
        byte[] data = "我是你大爷!".getBytes(CharsetUtil.UTF_8);

        var firstHeapUnpooledBuf = Unpooled.buffer(128);
        LOGGER.info("Unpooled Heap Buf: {} ", firstHeapUnpooledBuf );

        var firstHeapDirectBuf = Unpooled.directBuffer(128);
        LOGGER.info("Unpooled Direct Buf: {}", firstHeapDirectBuf);

        var pooledHeap = PooledByteBufAllocator.DEFAULT.buffer(128);
        LOGGER.info("Pooled Heap Buf: {}", pooledHeap);

        var pooledDirect = PooledByteBufAllocator.DEFAULT.directBuffer(128);
        LOGGER.info("Pooled Direct Buf : {}", pooledDirect);

        /*
        Write a known pattern into a buffer:

        E.g. write a few bytes/ints/strings in a fixed order.
         */
        LOGGER.info("Before writing to firstHeapUnpooledBuf: {}, readablebytes: {}", firstHeapUnpooledBuf, firstHeapUnpooledBuf.readableBytes());
        firstHeapUnpooledBuf.writeBytes("I am Batman".getBytes(CharsetUtil.UTF_8));
        LOGGER.info("After writing to firstHeapUnpooledBuf: {}, readablebytes: {}", firstHeapUnpooledBuf, firstHeapUnpooledBuf.readableBytes());

        LOGGER.info("Before writing to pooledDirect: {}, readablebytes: {}", pooledDirect, pooledDirect.readableBytes());
        pooledDirect.writeBytes("I am Batman".getBytes(CharsetUtil.UTF_8));
        LOGGER.info("After writing to pooledDirect: {}, readablebytes: {}", pooledDirect, pooledDirect.readableBytes());

        /*
        Experiment: read* vs get*
        Call readByte, readInt, etc. and log indices after each call.
        Then reset and call getByte, getInt on the same positions and log indices.
        Goal: see that:
        read* advances readerIndex.
        get* does not.
         */
        int intValue = firstHeapUnpooledBuf.readInt();
        LOGGER.info("After Reading intvalue {} from first firstHeapUnpooledBuf: {}", intValue, firstHeapUnpooledBuf);
        byte curByte = pooledDirect.readByte();
        LOGGER.info("After reading bytevalue {} from pooledDirect: {}", curByte , pooledDirect);

        LOGGER.info("Reseting buffer read index...");
        firstHeapUnpooledBuf.resetReaderIndex();
        pooledDirect.resetReaderIndex();

        intValue = firstHeapUnpooledBuf.getInt(0);
        LOGGER.info("After Getting intvalue {} from first firstHeapUnpooledBuf: {}", intValue, firstHeapUnpooledBuf);
        curByte = pooledDirect.getByte(0);
        LOGGER.info("After reading bytevalue {} from pooledDirect: {}", curByte , pooledDirect);;

        /*
            Observe direct vs heap

            For each buffer:

            Log whether it’s direct (isDirect()).

            From this point on, always write down in comments/notes:

            “This buffer is: heap/direct, pooled/unpooled.”
         */
        LOGGER.info("firstHeapUnpooledBuf is heap buffer? {}", !firstHeapUnpooledBuf.isDirect());
        LOGGER.info("pooledDirect is Direct buffer? {} ", pooledDirect.isDirect() );

    }


}
