package disruptor.practice.common;

import disruptor.practice.oms.model.TaskObject;
import disruptor.practice.oms.model.TaskResponse;

@FunctionalInterface
public interface CompletionSink {
//    void onComplete(TaskObject task, String result, int partitionId);
    void onComplete(TaskObject task, TaskResponse resp);
}
