package com.github.amlewis.graphy.core;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by amlewis on 7/12/15.
 */
public class IfElseNode<ResultType> extends BaseNode<ResultType> {
  private final AbstractProcessingNode<Boolean> conditionNode;
  private final AbstractProcessingNode<ResultType> onTrueNode;
  private final AbstractProcessingNode<ResultType> onFalseNode;
  private final AtomicBoolean onTrueLazy;
  private final AtomicBoolean onFalseLazy;

  private IfElseNode(AbstractProcessingNode<Boolean> conditionNode, AbstractProcessingNode<ResultType> onTrueNode, boolean onTrueLazy, AbstractProcessingNode<ResultType> onFalseNode, boolean onFalseLazy) {
    this.conditionNode = conditionNode;
    this.onTrueNode = onTrueNode;
    this.onFalseNode = onFalseNode;
    this.onTrueLazy = new AtomicBoolean(onTrueLazy);
    this.onFalseLazy = new AtomicBoolean(onFalseLazy);
  }

  @Override
  public void activate() {
    conditionNode.activate(this);
    if (!onTrueLazy.get()) {
      onTrueNode.activate(this);
    }
    if (!onFalseLazy.get()) {
      onFalseNode.activate(this);
    }
  }

  @Override
  void onDependencyUpdated(BaseNode<?> dependency) {
    NodeResult<Boolean> conditionResult = conditionNode.getResult();
    if (conditionResult != null && !conditionResult.isException()) {
      if (conditionResult.getResult().booleanValue()) {
        if (onTrueLazy.compareAndSet(true, false)) {
          onTrueNode.activate(this);
        }
        setResult(onTrueNode.getResult());
      } else {
        if (onFalseLazy.compareAndSet(true, false)) {
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

  public static class Builder<ResultType> {
    private AbstractProcessingNode<Boolean> conditionNode = null;
    private AbstractProcessingNode<ResultType> onTrueNode = null;
    private AbstractProcessingNode<ResultType> onFalseNode = null;
    private boolean onTrueLazy = false;
    private boolean onFalseLazy = false;

    public Builder() {
    }

    public Builder conditionNode(AbstractProcessingNode<Boolean> conditionNode) {
      this.conditionNode = conditionNode;
      return this;
    }

    public Builder onTrueNode(AbstractProcessingNode<ResultType> onTrueNode) {
      return onTrueNode(onTrueNode, false);
    }

    public Builder onTrueNode(AbstractProcessingNode<ResultType> onTrueNode, boolean lazy) {
      this.onTrueNode = onTrueNode;
      return this;
    }

    public Builder onFalseNode(AbstractProcessingNode<ResultType> onFalseNode) {
      return onFalseNode(onFalseNode, false);
    }

    public Builder onFalseNode(AbstractProcessingNode<ResultType> onFalseNode, boolean lazy) {
      this.onFalseNode = onFalseNode;
      return this;
    }

    public IfElseNode<ResultType> build() {
      return new IfElseNode<ResultType>(conditionNode, onTrueNode, onTrueLazy, onFalseNode, onFalseLazy);
    }
  }
}
