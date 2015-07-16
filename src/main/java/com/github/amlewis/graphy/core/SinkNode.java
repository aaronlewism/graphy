package com.github.amlewis.graphy.core;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by amlewis on 7/11/15.
 */
class SinkNode<ResultType> extends ProcessingNode<Void> {
  public static <ResultType> void sink(BaseNode<ResultType> nodeToSink, SinkCallback<ResultType> callback) {
    new SinkNode<>(nodeToSink, callback);
  }

  private BaseNode<ResultType> nodeToSink;
  private SinkCallback<ResultType> callback;

  public SinkNode(BaseNode<ResultType> nodeToSink, SinkCallback<ResultType> callback) {
    this.nodeToSink = nodeToSink;
    this.callback = callback;
    this.callback.addRef(this);
    activate();
  }


  @Override
  public void activate() {
    nodeToSink.activate(this);
    update();
  }

  @Override
  void activate(BaseNode<?> activator) {
    throw new UnsupportedOperationException("Cannot have nodes that depend on Sink Nodes!");
  }

  @Override
  void onDependencyUpdated(BaseNode<?> dependency) {
    update();
  }

  void deactivateSink() {
    nodeToSink.deactivate(this);
  }

  @Override
  void process() {
    NodeResult<ResultType> result = nodeToSink.getResult();
    if (result != null) {
      if (result.isException()) {
        callback.onException(result.getException());
      } else {
        callback.onResult(result.getResult());
      }
    }
  }

}
