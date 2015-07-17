package com.github.amlewis.graphy.core;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by amlewis on 7/15/15.
 * SinkCallbacks handle extracting results out of Graphy.
 * It is important to hold a strong reference to the SinkCallback in order to prevent it's associated SinkNode from
 * getting garbage collected.
 */
public abstract class SinkCallback<ResultType> {
  // Keeps strong references to sink nodes so they don't get cleaned up.
  private AtomicReference<SinkNode<ResultType>> nodeRef = new AtomicReference<>(null);

  void addRef(SinkNode<ResultType> node) {
    if (!nodeRef.compareAndSet(null, node)) {
      throw new IllegalStateException("A SinkCallback object can only be set once!");
    }
  }

  protected abstract void onNewResult(ResultType result);

  protected abstract void onNewException(Exception exception);

  protected abstract void onUnset();

  public final void deregister() {
    SinkNode<ResultType> node = nodeRef.get();
    if (node != null) {
      node.deactivateSink();
    }
  }

  static class FutureSinkCallback<ResultType> extends SinkCallback<ResultType> implements Future<ResultType> {
    private ArrayBlockingQueue<NodeResult<ResultType>> resultQueue = new ArrayBlockingQueue<>(1);
    private AtomicBoolean cancelled = new AtomicBoolean(false);

    @Override
    public void onNewResult(ResultType result) {
      this.resultQueue.offer(new NodeResult<ResultType>(result));
    }

    @Override
    public void onNewException(Exception exception) {
      this.resultQueue.offer(new NodeResult<ResultType>(exception));
    }

    @Override
    protected void onUnset() {
      // Do nothing. Since FutureSinkCallback is used exclusively after FirstValueNode, and FirstValueNode never calls unset, we should be fine doing nothing.
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
      // TODO: Delay between take and offer may lead to a different result being placed instead or a bad call to isDone()
      NodeResult<ResultType> result = resultQueue.take();
      resultQueue.offer(result);
      if (result.isException()) {
        throw new ExecutionException(result.getException());
      }
      return result.getResult();
    }

    @Override
    public ResultType get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      // TODO: Delay between take and offer may lead to a different result being placed instead or a bad call to isDone()
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
