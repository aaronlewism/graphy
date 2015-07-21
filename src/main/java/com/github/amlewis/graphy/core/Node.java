package com.github.amlewis.graphy.core;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by amlewis on 7/12/15.
 */
abstract class Node<ResultType> {
  private final Set<Node<?>> parents = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<Node<?>, Boolean>()));
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

  NodeResult<ResultType> getResult() {
    return result;
  }

  private AtomicBoolean isActive = new AtomicBoolean(false);

  // TODO: Race conditions between activate and deactivate!
  void activate(Node<?> activator) {
    if (activator != null) {
      parents.add(activator);
    }
    if (isActive.compareAndSet(false, true)) {
      activate();
    }
  }

  protected abstract void activate();

  void deactivate(Node<?> deactivator) {
    parents.remove(deactivator);
//    if (parents.isEmpty() && isActive.compareAndSet(true, false)) {
//      deactivate();
//    }
  }

  //protected abstract void deactivate();

  abstract void onDependencyUpdated(Node<?> dependency);

  void setResult(NodeResult<ResultType> result) {
    if (this.result != result && (this.result == null || !this.result.equals(result))) {
      this.result = result;
      notifyParents();
    }
  }

  void setResult(ResultType result) {
    setResult(new NodeResult<ResultType>(result));
  }

  void setResult(Exception exception) {
    setResult(new NodeResult<ResultType>(exception));
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
      Iterator<Node<?>> parentsIterator = new ArrayList<Node<?>>(parents).iterator();
      while (parentsIterator.hasNext()) {
        final Node<?> parent = parentsIterator.next();
        Graphy.getInstance().getGraphyExecutorService().execute(new Runnable() {
          @Override
          public void run() {
            parent.onDependencyUpdated(Node.this);
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
