package com.github.amlewis.graphy.core;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by amlewis on 7/10/15.
 */
public abstract class Node<ResultType> {
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

  public ResultType get() {
    if (result == null) {
      throw new NodeNotProcessedException("Node hasn't completed processing!");
    }

    if (result.isException()) {
      throw new NodeProcessingException("Node resulted in an exception!", result.getException());
    }

    return result.getResult();
  }

  private AtomicBoolean isUpdating = new AtomicBoolean(false);
  private AtomicBoolean needsUpdating = new AtomicBoolean(false);

  void update() {
    boolean shouldStartThread;
    synchronized (updateNodeRunnable) {
      needsUpdating.set(true);
      shouldStartThread = isUpdating.compareAndSet(false, true);
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
          if (!needsUpdating.compareAndSet(true, false)) {
            isUpdating.set(false);
            return;
          }
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

  /**
   * @return ResultType - Returns result of processing
   * @throws Exception - any exception that occurs during processing
   */
  protected abstract ResultType process() throws Exception;

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

  public class NodeNotProcessedException extends RuntimeException {
    public NodeNotProcessedException(String message) {
      super(message);
    }
  }

  public class NodeProcessingException extends RuntimeException {
    public NodeProcessingException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
