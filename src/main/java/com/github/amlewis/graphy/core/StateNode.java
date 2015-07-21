package com.github.amlewis.graphy.core;

import java.util.*;

/**
 * Created by amlewis on 7/10/15.
 */
public abstract class StateNode<ResultType> extends ProcessingNode<ResultType> {
  private final Set<Node<?>> dependencies;
  private final Set<Node<?>> unreadyDependencies;
  private final Set<Node<?>> exceptionalDependencies;

  public StateNode(Node<?>... dependencies) {
    this.dependencies = Collections.synchronizedSet(new HashSet<Node<?>>());
    this.unreadyDependencies = Collections.synchronizedSet(new HashSet<Node<?>>());
    this.exceptionalDependencies = Collections.synchronizedSet(new HashSet<Node<?>>());
    for (Node<?> node : dependencies) {
      this.dependencies.add(node);
    }
  }

  public StateNode(Collection<Node<?>> dependencies) {
    int size = dependencies.size();
    this.dependencies = Collections.synchronizedSet(new HashSet<Node<?>>());
    this.unreadyDependencies = Collections.synchronizedSet(new HashSet<Node<?>>());
    this.exceptionalDependencies = Collections.synchronizedSet(new HashSet<Node<?>>());
    this.dependencies.addAll(dependencies);
  }

  public void activate() {
    // TODO: Wasteful
    for (Node<?> dependency : dependencies) {
      dependency.activate(this);
      unreadyDependencies.add(dependency);
    }

    for (Node<?> dependency : dependencies) {
      onDependencyUpdated(dependency);
    }
  }


  @Override
  void process() {
    ResultType processResult = null;
    Exception exception = null;
    try {
      processResult = processResult();
    } catch (Exception e) {
      processResult = null;
      exception = e;
    }

    if (!shouldCancel()) {
      if (exception != null && exception instanceof Node.NodeNotProcessedException) {
        setResult((NodeResult<ResultType>) null);
      } else if (exception != null && exceptionalDependencies.isEmpty()) {
        setResult(exception);
      } else if (exception == null) {
        setResult(processResult);
      }
    }
  }

  /**
   * @return ResultType - Returns result of processing
   * @throws Exception - any exception that occurs during processing
   */
  protected abstract ResultType processResult() throws Exception;

  void onDependencyUpdated(Node<?> dependency) {
    if (dependencies.contains(dependency)) {
      NodeResult<?> dependencyResult = dependency.getResult();
      if (dependencyResult != null) {
        unreadyDependencies.remove(dependency);
        if (dependencyResult.isException()) {
          cancel();
          exceptionalDependencies.add(dependency);
          setResult(dependencyResult.getException());
        } else {
          exceptionalDependencies.remove(dependency);
          if (exceptionalDependencies.isEmpty() && unreadyDependencies.isEmpty()) {
            update();
          }
        }
      } else {
        cancel();
        setResult((NodeResult<ResultType>) null);
        unreadyDependencies.add(dependency);
      }
    }
  }
}
