package disruptor.practice.oms.handlers;

import com.lmax.disruptor.EventHandler;
import disruptor.practice.oms.model.TaskEvent;
import disruptor.practice.oms.model.TaskResponse;

/**
 * The only Consumer of Disruptor Events
 * @param partitionId
 */
public record DisruptorBusinessHandler(int partitionId) implements EventHandler<TaskEvent> {

    private final static String ROUTED = "ROUTED";
    private final static String PARKED = "PARKED";
    private final static String TARGET = "TARGET";

    @Override
    public void onEvent(TaskEvent event, long sequence, boolean endOfBatch) throws Exception {
        // last step
        String decision = switch ((int) (event.getOrderId() % 3)) {
            case 1 -> ROUTED;
            case 2 -> PARKED;
            default -> TARGET;
        };

        // asynchronously write back
        event.getChannel().write(new TaskResponse(event.getCorrelationId(), partitionId, decision,
                System.nanoTime(), event.getPayload()));
        if (endOfBatch) {
            event.getChannel().flush();
        }
    }
}
