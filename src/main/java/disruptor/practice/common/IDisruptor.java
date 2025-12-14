package disruptor.practice.common;

import java.nio.ByteBuffer;

public interface IDisruptor<T> {

    void translate(T event, long seq, ByteBuffer byteBuffer);
}
