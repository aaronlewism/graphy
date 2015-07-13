package com.github.amlewis.graphy.core;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by amlewis on 7/12/15.
 */
abstract class BaseNode<ResultType> {
  private final List<WeakReference<BaseNode<?>>> parents = new LinkedList<>();
  private NodeResult<ResultType> result = null;

  public ResultType get() {
    NodeResult<ResultType> result = this.result;
    if (result == null) {
      throw new NodeNotProcessedException("AbstractProcessingNode hasn't completed processing!");
    }

    if (result.isException()) {
      throw new NodeProcessingException("AbstractProcessingNode resulted in an exception!", result.getException());
    }

    return result.getResult();
  }

  public Exception getException() {
    NodeResult<ResultType> result = this.result;
    if (result == null) {
      throw new NodeNotProcessedException("AbstractProcessingNode hasn't completed processing!");
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

  void activate(BaseNode<?> activator) {
    if (activator != null) {
      parents.add(new WeakReference<BaseNode<?>>(activator));
    }
    activate();
  }

  abstract void onDependencyUpdated(BaseNode<?> dependency);

  void notifyParents() {
    Iterator<WeakReference<BaseNode<?>>> parentsIterator = parents.iterator();
    while (parentsIterator.hasNext()) {
      WeakReference<BaseNode<?>> parentRef = parentsIterator.next();
      BaseNode<?> parent = parentRef.get();
      if (parent == null) {
        parentsIterator.remove();
      } else {
        parent.onDependencyUpdated(this);
      }
    }
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
