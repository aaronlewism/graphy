package com.github.amlewis.graphy.core;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by amlewis on 7/15/15.
 */
public final class FirstValueNode<ResultType> extends Node<ResultType> {
  public static <ResultType> FirstValueNode<ResultType> wrap(Node<ResultType> child) {
    return new FirstValueNode<ResultType>(child);
  }

  private Node<ResultType> child;
  private AtomicBoolean resultSet = new AtomicBoolean(false);

  public FirstValueNode(Node<ResultType> child) {
    this.child = child;
  }

  @Override
  public void activate() {
    child.activate(this);
    onDependencyUpdated(child);
  }

  @Override
  void onDependencyUpdated(Node<?> dependency) {
    if (!resultSet.get()) {
      NodeResult<ResultType> result = child.getResult();
      if (result != null && resultSet.compareAndSet(false, true)) {
        setResult(result);
        child.deactivate(this);
        child = null;
      }
    }
  }
}
