package disruptor.practice.common;

public interface IPerEventLogging<T> {

    default void perEventLogging(T event, long seq, int rate) {
        // do nothing
    }
}
