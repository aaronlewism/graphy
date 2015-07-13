package com.github.amlewis.graphy.core;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by amlewis on 7/12/15.
 */
public class TryCatchNode<ResultType> extends BaseNode<ResultType> {
  private final AbstractProcessingNode<ResultType> mainNode;
  private final AbstractProcessingNode<ResultType> onExceptionNode;
  private final Class<?> exceptionClass;
  private final AtomicBoolean lazy;

  public TryCatchNode(AbstractProcessingNode<ResultType> mainNode, AbstractProcessingNode<ResultType> onExceptionNode, Class<?> exceptionClass, boolean lazy) {
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
  }

  @Override
  void onDependencyUpdated(BaseNode<?> dependency) {
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
}
