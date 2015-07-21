package com.github.amlewis.graphy.core;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by amlewis on 7/12/15.
 */
public final class TryNode<ResultType> extends ProcessingNode<ResultType> {
  private final Node<ResultType> mainNode;
  private final Node<ResultType> onExceptionNode;
  private final Class<? extends Exception> exceptionClass;
  private final AtomicBoolean lazy;

  public TryNode(Node<ResultType> mainNode, Node<ResultType> onExceptionNode, boolean lazy, Class<? extends Exception> exceptionClass) {
    this.mainNode = mainNode;
    this.onExceptionNode = onExceptionNode;
    this.exceptionClass = exceptionClass;
    this.lazy = new AtomicBoolean(lazy);
  }

  @Override
  public void activate() {
    mainNode.activate(this);
    if (!lazy.get()) {
      onExceptionNode.activate(this);
    }
    update();
  }

  @Override
  void process() {
    NodeResult<ResultType> mainNodeResult = mainNode.getResult();
    if (mainNodeResult != null && !mainNodeResult.isException()) {
      setResult(mainNodeResult.getResult());
    } else if (mainNodeResult != null && mainNodeResult.isException()) {
      Exception mainException = mainNodeResult.getException();
      if (exceptionClass == null || exceptionClass.isInstance(mainException)) {
        if (lazy.compareAndSet(true, false)) {
          onExceptionNode.activate(this);
        }
        NodeResult<ResultType> onExceptionResult = onExceptionNode.getResult();
        setResult(onExceptionResult);
      } else {
        setResult(mainException);
      }
    } else {
      setResult((NodeResult<ResultType>) null);
    }
  }

  @Override
  void onDependencyUpdated(Node<?> dependency) {
    update();
  }

  public static class Builder<ResultType> {
    private Node<ResultType> mainNode;
    private Node<ResultType> onExceptionNode;
    private boolean lazy;
    private Class<? extends Exception> exceptionClass;

    public Builder() {
    }

    public Builder mainNode(Node<ResultType> mainNode) {
      this.mainNode = mainNode;
      return this;
    }

    public Builder onExceptionNode(Node<ResultType> onExceptionNode) {
      return onExceptionNode(onExceptionNode, false);
    }

    public Builder onExceptionNode(Node<ResultType> onExceptionNode, boolean lazy) {
      this.onExceptionNode = onExceptionNode;
      this.lazy = lazy;
      return this;
    }

    public Builder exceptionClass(Class<? extends Exception> exceptionClass) {
      this.exceptionClass = exceptionClass;
      return this;
    }

    public TryNode<ResultType> build() {
      return new TryNode<ResultType>(mainNode, onExceptionNode, lazy, exceptionClass);
    }

  }
}
