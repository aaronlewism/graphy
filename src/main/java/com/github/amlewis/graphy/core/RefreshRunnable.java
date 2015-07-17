package com.github.amlewis.graphy.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by amlewis on 7/13/15.
 */
abstract class RefreshRunnable implements Runnable {
  private volatile boolean shouldCancel = false;
  private volatile boolean isUpdating = false;
  private volatile boolean needsUpdating = false;
  private volatile Future<?> runnableFuture = null;

  /**
   * Marks ndoe as needing a refresh, executing if needed.
   */
  public void refresh(ExecutorService executorService) {
    boolean shouldStartThread;
    synchronized (this) {
      needsUpdating = true;
      shouldStartThread = !isUpdating;
      isUpdating = true;
      if (shouldStartThread) {
        shouldCancel = false;
      }
    }
    if (shouldStartThread) {
      runnableFuture = executorService.submit(this);
    }
  }

  @Override
  public void run() {
    while (true) {
      synchronized (this) {
        shouldCancel = false;
        if (!needsUpdating) {
          runnableFuture = null;
          isUpdating = false;
          return;
        }
        needsUpdating = false;
      }

      try {
        work();
      } catch (Exception e) {
        synchronized (this) {
          runnableFuture = null;
          isUpdating = false;
        }
        throw e;
      }
    }
  }

  public abstract void work();

  public void cancel() {
    cancel(false);
  }

  public void cancel(boolean mayInterruptIfRunning) {
    synchronized (this) {
      if (!isUpdating) {
        shouldCancel = true;
        needsUpdating = false;
      }
      if (runnableFuture != null) {
        runnableFuture.cancel(mayInterruptIfRunning);
      }
    }
  }

  protected boolean shouldCancel() {
    return shouldCancel;
  }
}
