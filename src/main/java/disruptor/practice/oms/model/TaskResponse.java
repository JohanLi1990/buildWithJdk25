package disruptor.practice.oms.model;

import io.netty.buffer.ByteBuf;

public record TaskResponse(long correlationId, int partitionId, String decision, long t1, String originalPayload) {
}
