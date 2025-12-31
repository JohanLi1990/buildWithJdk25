package disruptor.practice.common;

import disruptor.practice.oms.model.TaskObject;

@FunctionalInterface
public interface CompletionSink {
    void onComplete(TaskObject task, String result, int partitionId);
}
