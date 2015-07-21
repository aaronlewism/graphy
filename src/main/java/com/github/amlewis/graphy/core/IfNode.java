package com.github.amlewis.graphy.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by amlewis on 7/12/15.
 */
public final class IfNode<ResultType> extends ProcessingNode<ResultType> {
  private final StateNode<Boolean> conditionNode;
  private final StateNode<ResultType> onTrueNode;
  private final StateNode<ResultType> onFalseNode;
  private final AtomicBoolean OnTrueShouldActivate;
  private final AtomicBoolean onFalseShouldActivate;

  private IfNode(StateNode<Boolean> conditionNode, StateNode<ResultType> onTrueNode, boolean onTrueLazy, StateNode<ResultType> onFalseNode, boolean onFalseLazy) {
    this.conditionNode = conditionNode;
    this.onTrueNode = onTrueNode;
    this.onFalseNode = onFalseNode;
    this.OnTrueShouldActivate = new AtomicBoolean(onTrueLazy);
    this.onFalseShouldActivate = new AtomicBoolean(onFalseLazy);
  }

  @Override
   protected void activate() {
    conditionNode.activate(this);
    if (!OnTrueShouldActivate.get()) {
      onTrueNode.activate(this);
    }
    if (!onFalseShouldActivate.get()) {
      onFalseNode.activate(this);
    }
    update();
  }

  @Override
  void onDependencyUpdated(Node<?> dependency) {
    update();
  }

  @Override
  void process() {
    NodeResult<Boolean> conditionResult = conditionNode.getResult();
    if (conditionResult != null && !conditionResult.isException()) {
      if (conditionResult.getResult().booleanValue()) {
        if (OnTrueShouldActivate.compareAndSet(true, false)) {
          onTrueNode.activate(this);
        }
        setResult(onTrueNode.getResult());
      } else {
        if (onFalseShouldActivate.compareAndSet(true, false)) {
          onFalseNode.activate(this);
        }
        setResult(onFalseNode.getResult());
      }
    } else if (conditionResult != null && conditionResult.isException()) {
      setResult(conditionResult.getException());
    } else {
      setResult((NodeResult<ResultType>) null);
    }
  }

  // TODO: Builder constructor should always require any required nodes?
  public static class Builder<ResultType> {
    private StateNode<Boolean> conditionNode = null;
    private StateNode<ResultType> onTrueNode = null;
    private StateNode<ResultType> onFalseNode = null;
    private ExecutorService executorService = null;
    private boolean onTrueLazy = false;
    private boolean onFalseLazy = false;

    public Builder() {
    }

    public Builder conditionNode(StateNode<Boolean> conditionNode) {
      this.conditionNode = conditionNode;
      return this;
    }

    public Builder onTrueNode(StateNode<ResultType> onTrueNode) {
      return onTrueNode(onTrueNode, false);
    }

    public Builder onTrueNode(StateNode<ResultType> onTrueNode, boolean lazy) {
      this.onTrueNode = onTrueNode;
      return this;
    }

    public Builder onFalseNode(StateNode<ResultType> onFalseNode) {
      return onFalseNode(onFalseNode, false);
    }

    public Builder onFalseNode(StateNode<ResultType> onFalseNode, boolean lazy) {
      this.onFalseNode = onFalseNode;
      return this;
    }

    public Builder executorService(ExecutorService executorService) {
      this.executorService = executorService;
      return this;
    }

    public IfNode<ResultType> build() {
      IfNode<ResultType> node = new IfNode<ResultType>(conditionNode, onTrueNode, onTrueLazy, onFalseNode, onFalseLazy);
      node.setExecutorService(executorService);
      return node;
    }
  }
}
