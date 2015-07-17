package com.github.amlewis.graphy.core;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by amlewis on 7/15/15.
 */
public final class FirstValueNode<ResultType> extends BaseNode<ResultType> {
  public static <ResultType> FirstValueNode<ResultType> wrap(BaseNode<ResultType> child) {
    return new FirstValueNode<>(child);
  }

  private BaseNode<ResultType> child;
  private AtomicBoolean resultSet = new AtomicBoolean(false);

  public FirstValueNode(BaseNode<ResultType> child) {
    this.child = child;
  }

  @Override
  public void activate() {
    child.activate(this);
    onDependencyUpdated(child);
  }

  @Override
  void onDependencyUpdated(BaseNode<?> dependency) {
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
