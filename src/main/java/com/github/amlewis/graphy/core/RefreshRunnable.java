package com.github.amlewis.graphy.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by amlewis on 7/13/15.
 */
public abstract class RefreshRunnable implements Runnable {
  private boolean shouldCancel = false;
  private boolean isUpdating = false;
  private boolean needsUpdating = false;
  private Future<?> runnableFuture;

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
          isUpdating = false;
          runnableFuture = null;
          return;
        }
        needsUpdating = false;
      }

      work();
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
