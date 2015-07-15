package com.github.amlewis.graphy.core;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by amlewis on 7/11/15.
 */
public class SinkNode<ResultType> extends ProcessingNode<ResultType> {
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

  public static abstract class SinkCallback<ResultType> {
    // Keeps strong references to sink nodes so they don't get cleaned up.
    private AtomicReference<SinkNode<ResultType>> nodeRef = new AtomicReference<>(null);

    void addRef(SinkNode<ResultType> node) {
      if (!nodeRef.compareAndSet(null, node)) {
        throw new IllegalStateException("A SinkCallback object can only be set once!");
      }
    }

    public abstract void onResult(ResultType result);

    public abstract void onException(Exception exception);

    final public void deregister() {
      SinkNode<ResultType> node = nodeRef.get();
      if (node != null) {
        node.deactivateSink();
      }
    }
  }

  static class FutureSinkCallback<ResultType> extends SinkCallback<ResultType> implements Future<ResultType> {
    private ArrayBlockingQueue<NodeResult<ResultType>> resultQueue = new ArrayBlockingQueue<>(1);
    private AtomicBoolean cancelled = new AtomicBoolean(false);

    @Override
    public void onResult(ResultType result) {
      this.resultQueue.offer(new NodeResult<ResultType>(result));
    }

    @Override
    public void onException(Exception exception) {
      this.resultQueue.offer(new NodeResult<ResultType>(exception));
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      // TODO: Support cancelling
      return false;
    }

    @Override
    public boolean isCancelled() {
      // TODO: Support cancelling
      return false;
    }

    @Override
    public boolean isDone() {
      return !resultQueue.isEmpty();
    }

    @Override
    public ResultType get() throws InterruptedException, ExecutionException {
      NodeResult<ResultType> result = resultQueue.take();
      resultQueue.offer(result);
      if (result.isException()) {
        throw new ExecutionException(result.getException());
      }
      return result.getResult();
    }

    @Override
    public ResultType get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      NodeResult<ResultType> result = resultQueue.take();
      if (result == null) {
        throw new TimeoutException();
      }
      resultQueue.offer(result, timeout, unit);
      if (result.isException()) {
        throw new ExecutionException(result.getException());
      }
      return result.getResult();
    }
  }
}
