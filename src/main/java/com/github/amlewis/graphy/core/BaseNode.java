package com.github.amlewis.graphy.core;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by amlewis on 7/12/15.
 */
abstract class BaseNode<ResultType> {
  private final Set<BaseNode<?>> parents = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<BaseNode<?>, Boolean>()));
  private NodeResult<ResultType> result = null;

  public ResultType get() {
    NodeResult<ResultType> result = this.result;
    if (result == null) {
      throw new NodeNotProcessedException("Node hasn't completed processing!");
    }

    if (result.isException()) {
      throw new NodeProcessingException("Node resulted in an exception!", result.getException());
    }

    return result.getResult();
  }

  public Exception getException() {
    NodeResult<ResultType> result = this.result;
    if (result == null) {
      throw new NodeNotProcessedException("Node hasn't completed processing!");
    }

    return result.getException();
  }

  NodeResult<ResultType> getResult() {
    return result;
  }

  public boolean isReady() {
    return result != null;
  }

  public boolean isSuccess() {
    NodeResult<ResultType> result = this.result;
    return result != null && !result.isException();
  }

  public boolean isException() {
    NodeResult<ResultType> result = this.result;
    return result != null && result.isException();
  }

  private AtomicBoolean isActive = new AtomicBoolean(false);

  void activate(BaseNode<?> activator) {
    if (activator != null) {
      parents.add(activator);
    }
    if (isActive.compareAndSet(false, true)) {
      activate();
    }
  }

  protected abstract void activate();

  void deactivate(BaseNode<?> deactivator) {
    parents.remove(deactivator);
  }

  abstract void onDependencyUpdated(BaseNode<?> dependency);

  void setResult(NodeResult<ResultType> result) {
    if (this.result != result && (this.result == null || !this.result.equals(result))) {
      this.result = result;
      notifyParents();
    }
  }

  void setResult(ResultType result) {
    if (this.result == null || (!this.result.getResult().equals(result))) {
      this.result = new NodeResult<ResultType>(result);
      notifyParents();
    }
  }

  void setResult(Exception exception) {
    if (this.result == null || (!this.result.getException().equals(exception))) {
      this.result = new NodeResult<ResultType>(exception);
      notifyParents();
    }
  }

  // TODO: Enqueue updated nodes instead of calling onDependencyUpdated on multiple threads?
  // TODO: This spawns a LOT of threads. (n + 1 where n is number of parents)
  void notifyParents() {
    notifyParentsRunnable.refresh(Graphy.getInstance().getGraphyExecutorService());
  }

  private final NotifyParentsRunnable notifyParentsRunnable = new NotifyParentsRunnable();
  private class NotifyParentsRunnable extends RefreshRunnable {
    @Override
    public void work() {
      Iterator<BaseNode<?>> parentsIterator = new ArrayList<BaseNode<?>>(parents).iterator();
      while (parentsIterator.hasNext()) {
        final BaseNode<?> parent = parentsIterator.next();
        Graphy.getInstance().getGraphyExecutorService().execute(new Runnable() {
          @Override
          public void run() {
            parent.onDependencyUpdated(BaseNode.this);
          }
        });
      }
    }
  }

  public static class NodeNotProcessedException extends RuntimeException {
    public NodeNotProcessedException(String message) {
      super(message);
    }
  }

  public static class NodeProcessingException extends RuntimeException {
    public NodeProcessingException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
