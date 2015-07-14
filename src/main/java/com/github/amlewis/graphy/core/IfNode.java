package com.github.amlewis.graphy.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by amlewis on 7/12/15.
 */
public class IfNode<ResultType> extends ProcessingNode<ResultType> {
  private final MultiDependencyNode<Boolean> conditionNode;
  private final MultiDependencyNode<ResultType> onTrueNode;
  private final MultiDependencyNode<ResultType> onFalseNode;
  private final AtomicBoolean onTrueLazy;
  private final AtomicBoolean onFalseLazy;

  private IfNode(MultiDependencyNode<Boolean> conditionNode, MultiDependencyNode<ResultType> onTrueNode, boolean onTrueLazy, MultiDependencyNode<ResultType> onFalseNode, boolean onFalseLazy) {
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
    update();
  }

  @Override
  void process() {
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
    private MultiDependencyNode<Boolean> conditionNode = null;
    private MultiDependencyNode<ResultType> onTrueNode = null;
    private MultiDependencyNode<ResultType> onFalseNode = null;
    private ExecutorService executorService = null;
    private boolean onTrueLazy = false;
    private boolean onFalseLazy = false;

    public Builder() {
    }

    public Builder conditionNode(MultiDependencyNode<Boolean> conditionNode) {
      this.conditionNode = conditionNode;
      return this;
    }

    public Builder onTrueNode(MultiDependencyNode<ResultType> onTrueNode) {
      return onTrueNode(onTrueNode, false);
    }

    public Builder onTrueNode(MultiDependencyNode<ResultType> onTrueNode, boolean lazy) {
      this.onTrueNode = onTrueNode;
      return this;
    }

    public Builder onFalseNode(MultiDependencyNode<ResultType> onFalseNode) {
      return onFalseNode(onFalseNode, false);
    }

    public Builder onFalseNode(MultiDependencyNode<ResultType> onFalseNode, boolean lazy) {
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
