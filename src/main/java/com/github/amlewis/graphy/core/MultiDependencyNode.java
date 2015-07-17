package com.github.amlewis.graphy.core;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by amlewis on 7/10/15.
 */
public abstract class MultiDependencyNode<ResultType> extends ProcessingNode<ResultType> {
  private final Set<BaseNode<?>> dependencies;
  private final Set<BaseNode<?>> unreadyDependencies;
  private final Set<BaseNode<?>> exceptionalDependencies;

  public MultiDependencyNode(BaseNode<?>... dependencies) {
    this.dependencies = Collections.synchronizedSet(new HashSet<BaseNode<?>>());
    this.unreadyDependencies = Collections.synchronizedSet(new HashSet<BaseNode<?>>());
    this.exceptionalDependencies = Collections.synchronizedSet(new HashSet<BaseNode<?>>());
    for (BaseNode<?> node : dependencies) {
      this.dependencies.add(node);
    }
  }

  public MultiDependencyNode(Collection<BaseNode<?>> dependencies) {
    int size = dependencies.size();
    this.dependencies = Collections.synchronizedSet(new HashSet<BaseNode<?>>());
    this.unreadyDependencies = Collections.synchronizedSet(new HashSet<BaseNode<?>>());
    this.exceptionalDependencies = Collections.synchronizedSet(new HashSet<BaseNode<?>>());
    this.dependencies.addAll(dependencies);
  }

  public void activate() {
    // TODO: Wasteful
    for (BaseNode<?> dependency : dependencies) {
      dependency.activate(this);
      unreadyDependencies.add(dependency);
    }

    for (BaseNode<?> dependency : dependencies) {
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
      if (exception != null && exception instanceof BaseNode.NodeNotProcessedException) {
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

  void onDependencyUpdated(BaseNode<?> dependency) {
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
