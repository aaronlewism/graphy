package com.github.amlewis.graphy.core;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by amlewis on 7/10/15.
 */
public abstract class AbstractProcessingNode<ResultType> extends BaseNode<ResultType> {
  private final List<BaseNode<?>> dependencies = new LinkedList<>();
  private final Set<BaseNode<?>> unreadyDependencies = new ConcurrentSkipListSet<>();
  private final Set<BaseNode<?>> exceptionalDependencies = new ConcurrentSkipListSet<>();
  private NodeResult<ResultType> result = null;
  private ExecutorService executorService = null;

  public AbstractProcessingNode(BaseNode<?>... dependencies) {
    for (BaseNode<?> node : dependencies) {
      this.dependencies.add(node);
    }
  }

  public AbstractProcessingNode(Collection<BaseNode<?>> dependencies) {
    this.dependencies.addAll(dependencies);
  }

  private AtomicBoolean isActive = new AtomicBoolean(false);

  public void activate() {
    if (isActive.compareAndSet(false, true)) {
      // TODO: Wasteful
      for (BaseNode<?> dependency : dependencies) {
        dependency.activate(this);
        unreadyDependencies.add(dependency);
      }

      for (BaseNode<?> dependency : dependencies) {
        onDependencyUpdated(dependency);
      }
    }
  }

  void update() {
    boolean shouldStartThread = updateNodeRunnable.needsUpdate();

    if (shouldStartThread) {
      if (executorService == null) {
        Graphy.getInstance().getDefaultProcessingExecutorService().execute(updateNodeRunnable);
      } else {
        executorService.execute(updateNodeRunnable);
      }
    }
  }

  private class UpdateNodeRunnable implements Runnable {
    private AtomicBoolean shouldCancel = new AtomicBoolean(false);
    private boolean isUpdating = false;
    private boolean needsUpdating = false;

    public boolean needsUpdate() {
      boolean shouldStartThread;
      synchronized (this) {
        needsUpdating = true;
        shouldStartThread = !isUpdating;
        isUpdating = true;
        if (shouldStartThread) {
          shouldCancel.set(false);
        }
      }
      return shouldStartThread;
    }

    @Override
    public void run() {
      while (true) {
        synchronized (this) {
          if (!needsUpdating) {
            isUpdating = false;
            return;
          }
          needsUpdating = false;
        }

        ResultType processResult = null;
        Exception exception = null;
        try {
          processResult = process();
        } catch (Exception e) {
          processResult = null;
          exception = e;
        }

        if (!shouldCancel.compareAndSet(true, false)) {
          if (exception != null && exception instanceof NodeNotProcessedException) {
            setResult((NodeResult<ResultType>) null);
          } else if (exception != null && exceptionalDependencies.isEmpty()) {
            setResult(exception);
          } else if (exception == null) {
            setResult(processResult);
          }
        }
      }
    }

    public void cancel() {
      synchronized (this) {
        if (!isUpdating) {
          shouldCancel.set(true);
          needsUpdating = false;
        }
      }
    }
  }

  private final UpdateNodeRunnable updateNodeRunnable = new UpdateNodeRunnable();

  /**
   * @return ResultType - Returns result of processing
   * @throws Exception - any exception that occurs during processing
   */
  protected abstract ResultType process() throws Exception;

  void onDependencyUpdated(BaseNode<?> dependency) {
    NodeResult<?> dependencyResult = dependency.getResult();
    if (dependencyResult != null) {
      unreadyDependencies.remove(dependency);
      if (dependencyResult.isException()) {
        updateNodeRunnable.cancel();
        exceptionalDependencies.add(dependency);
        setResult(dependencyResult.getException());
      } else {
        exceptionalDependencies.remove(dependency);
        if (exceptionalDependencies.isEmpty() && unreadyDependencies.isEmpty()) {
          update();
        }
      }
    } else {
      updateNodeRunnable.cancel();
      setResult((NodeResult<ResultType>) null);
      unreadyDependencies.add(dependency);
    }
  }

}
