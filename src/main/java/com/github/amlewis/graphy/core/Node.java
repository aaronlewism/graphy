package com.github.amlewis.graphy.core;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by amlewis on 7/10/15.
 */
public abstract class Node<ResultType> {
  private final List<WeakReference<Node<?>>> parents = new LinkedList<>();
  private final List<Node<?>> dependencies = new LinkedList<>();
  private NodeResult<ResultType> result = null;

  public Node(Node<?>... dependencies) {
    for (Node<?> node : dependencies) {
      this.dependencies.add(node);
    }
  }

  public Node(Collection<Node<?>> dependencies) {
    this.dependencies.addAll(dependencies);
  }

  private AtomicBoolean isActive = new AtomicBoolean(false);

  public void activate() {
    activate(null);
  }

  void activate(Node<?> parent) {
    if (parent != null) {
      parents.add(new WeakReference<Node<?>>(parent));
    }
    if (isActive.compareAndSet(false, true)) {
      for (Node<?> dependency : dependencies) {
        dependency.activate(this);
      }
    }
  }

  private boolean isUpdating = false;
  private boolean needsUpdating = false;

  void update() {
    boolean shouldStartThread;
    synchronized (updateNodeRunnable) {
      needsUpdating = true;
      shouldStartThread = !isUpdating;
      isUpdating = true;
    }

    if (shouldStartThread) {
      Graphy.getInstance().getExecutorService().execute(updateNodeRunnable);
    }
  }

  private final Runnable updateNodeRunnable = new Runnable() {
    @Override
    public void run() {
      while (true) {
        synchronized (updateNodeRunnable) {
          if (!needsUpdating) {
            isUpdating = false;
            return;
          }
          needsUpdating = false;
        }

        ResultType processResult = null;
        Exception exception = null;
        try {
          processResult = process();
        } catch (Exception e) {
          processResult = null;
          exception = e;
        }

        if (exception != null) {
          setResult(exception);
        } else {
          setResult(processResult);
        }
      }
    }
  };

  void setResult(ResultType result) {
    this.result = new NodeResult<ResultType>(result);
  }

  void setResult(Exception exception) {
    this.result = new NodeResult<ResultType>(exception);
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

  public Exception getException() {
    NodeResult<ResultType> result = this.result;
    if (result == null) {
      throw new NodeNotProcessedException("Node hasn't completed processing!");
    }

    return result.getException();
  }

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

  /**
   * @return ResultType - Returns result of processing
   * @throws Exception - any exception that occurs during processing
   */
  protected abstract ResultType process() throws Exception;

  private void onDependencyUpdated(Node<?> dependency) {
    NodeResult<?> dependencyResult = dependency.getResult();
    if (dependencyResult != null) {
      if (dependencyResult.isException()) {
        // TODO: Cancel anything running somehow
        setResult(new NodeDependencyException(dependencyResult.getException()));
      } else {

      }
    }
  }

  static class NodeResult<ResultType> {
    private final ResultType result;
    private final Exception exception;

    public NodeResult(ResultType result) {
      this.result = result;
      this.exception = null;
    }

    public NodeResult(Exception exception) {
      if (exception == null) {
        throw new IllegalArgumentException("Exception cannot be null!");
      }
      this.result = null;
      this.exception = exception;
    }

    public ResultType getResult() {
      return result;
    }

    public Exception getException() {
      return exception;
    }

    public boolean isException() {
      return exception != null;
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

  public static class NodeDependencyException extends Exception {
    public NodeDependencyException(Throwable cause) {
      super(cause);
    }
  }
}
