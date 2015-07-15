package com.github.amlewis.graphy.core;

import java.util.Collection;

/**
 * Created by amlewis on 7/11/15.
 */
public class SinkNode<ResultType> extends ProcessingNode<ResultType> {
  private BaseNode<ResultType> nodeToSink;
  private SinkCallback<ResultType> callback;

  public SinkNode(BaseNode<ResultType> nodeToSink, SinkCallback<ResultType> callback) {
    this.nodeToSink = nodeToSink;
    this.callback = callback;
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

  public interface SinkCallback<ResultType> {
    void onResult(ResultType result);

    void onException(Exception exception);
  }
}
