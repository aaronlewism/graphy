package com.github.amlewis.graphy.core;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by amlewis on 7/16/15.
 */
public class GraphyTestUtils {
  static class SinkArrayBlockingQueue<ResultType> extends LinkedBlockingDeque<NodeResult<ResultType>> {
    private final QueueSinkCallback callback;

    public SinkArrayBlockingQueue(int capacity) {
      super(capacity);
      this.callback = new QueueSinkCallback(this);
    }

    private SinkCallback<ResultType> getCallback() {
      return callback;
    }

    class QueueSinkCallback extends SinkCallback<ResultType> {
      private final BlockingQueue<NodeResult<ResultType>> queue;

      public QueueSinkCallback(BlockingQueue<NodeResult<ResultType>> queue) {
        this.queue = queue;
      }

      @Override
      public void onNewResult(ResultType result) {
        queue.offer(new NodeResult<ResultType>(result));
      }

      @Override
      public void onNewException(Exception exception) {
        queue.offer(new NodeResult<ResultType>(exception));
      }

      @Override
      protected void onUnset() {
        // Skip unsets for now. Might be good to push nulls later?
      }
    }
  }

  static <ResultType> BlockingDeque<NodeResult<ResultType>> sinkToQueue(BaseNode<ResultType> node, int capacity) {
    SinkArrayBlockingQueue<ResultType> queueSink = new SinkArrayBlockingQueue<ResultType>(capacity);
    Graphy.sink(node, queueSink.getCallback());
    return queueSink;
  }
}
