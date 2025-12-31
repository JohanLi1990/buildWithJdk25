package disruptor.practice.oms.model;

public record TaskResponse(long orderId, long correlationId, int partitionId, int seqInFamily, String decision,
                           long t1) {
}
