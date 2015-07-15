package com.github.amlewis.graphy.core;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by amlewis on 7/12/15.
 */
abstract class BaseNode<ResultType> {
  private final ConcurrentSkipListSet<WeakReference<BaseNode<?>>> parents = new ConcurrentSkipListSet<>();
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

  public abstract void activate();

  void deactivate(BaseNode<?> deactivator) {
    Iterator<WeakReference<BaseNode<?>>> parentsIterator = parents.iterator();
    while (parentsIterator.hasNext()) {
      WeakReference<BaseNode<?>> parentRef = parentsIterator.next();
      final BaseNode<?> parent = parentRef.get();
      if (parent == null || parent == deactivator) {
        parentsIterator.remove();
      }
    }
  }

  private AtomicBoolean isActive = new AtomicBoolean(false);

  void activate(BaseNode<?> activator) {
    if (activator != null) {
      parents.add(new WeakReference<BaseNode<?>>(activator));
    }
    if (isActive.compareAndSet(false, true)) {
      activate();
    }
  }

  abstract void onDependencyUpdated(BaseNode<?> dependency);

  // TODO: Enqueue updated nodes instead of calling onDependencyUpdated on multiple threads?
  // TODO: This spawns a LOT of threads. (n + 1 where n is number of parents)
  void notifyParents() {
    Graphy.getInstance().getGraphyExecutorService().execute(new Runnable() {
      @Override
      public void run() {
        Iterator<WeakReference<BaseNode<?>>> parentsIterator = parents.iterator();
        while (parentsIterator.hasNext()) {
          WeakReference<BaseNode<?>> parentRef = parentsIterator.next();
          final BaseNode<?> parent = parentRef.get();
          if (parent == null || !parent.isActive.get()) {
            parentsIterator.remove();
          } else {
            Graphy.getInstance().getGraphyExecutorService().execute(new Runnable() {
              @Override
              public void run() {
                parent.onDependencyUpdated(BaseNode.this);
              }
            });
          }
        }
      }
    });
  }

  void setResult(NodeResult<ResultType> result) {
    if (this.result != result) {
      this.result = result;
      notifyParents();
    }
  }

  void setResult(ResultType result) {
    if (this.result != null && this.result.getResult() != result) {
      this.result = new NodeResult<ResultType>(result);
      notifyParents();
    }
  }

  void setResult(Exception exception) {
    if (this.result != null && this.result.getException() != exception) {
      this.result = new NodeResult<ResultType>(exception);
      notifyParents();
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
