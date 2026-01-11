package disruptor.practice.oms.model;

public record ConfigHolder(boolean ALLOC_HOTSPOT, boolean ALLOC_MITIGATION, boolean PENDING_CAP, int pendingLimit) {
}
