package com.github.amlewis.graphy.core;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by amlewis on 7/11/15.
 */
public class Graphy {

  private static Graphy instance;

  public static Graphy getInstance() {
    if (instance == null) {
      synchronized (Graphy.class) {
        if (instance == null) {
          instance = new Graphy();
        }
      }
    }

    return instance;
  }

  private volatile ExecutorService graphyExecutorService = Executors.newCachedThreadPool();

  ExecutorService getGraphyExecutorService() {
    return graphyExecutorService;
  }

  private volatile ExecutorService defaultProcessingExecutorService = graphyExecutorService;

  ExecutorService getDefaultProcessingExecutorService() {
    return defaultProcessingExecutorService;
  }

  public void setDefaultProcessingExecutorService(ExecutorService executorService) {
    this.defaultProcessingExecutorService = executorService;
  }

  // Sinking Nodes
  public static <ResultType> void sink(BaseNode<ResultType> node, SinkCallback<ResultType> callback) {
    new SinkNode<>(node, callback);
  }

  public static <ResultType> Future<ResultType> sinkFirstResultFuture(BaseNode<ResultType> node) {
    SinkCallback.FutureSinkCallback<ResultType> futureSinkCallback = new SinkCallback.FutureSinkCallback<>();
    SinkNode.sink(FirstValueNode.wrap(node), futureSinkCallback);
    return futureSinkCallback;
  }

  public static <ResultType> ResultType sinkFirstResult(BaseNode<ResultType> node) throws ExecutionException, InterruptedException {
    return sinkFirstResultFuture(node).get();
  }

  public static <ResultType> ResultType sinkFirstResult(BaseNode<ResultType> node, long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
    return sinkFirstResultFuture(node).get(timeout, unit);
  }

  public static <ResultType> BlockingDeque<NodeResult<ResultType>> sinkToBlockingDeque(BaseNode<ResultType> node, int capacity) {
    Graphy.SinkLinkedBlockingDeque<ResultType> queueSink = new Graphy.SinkLinkedBlockingDeque<ResultType>(capacity);
    Graphy.sink(node, queueSink.getCallback());
    return queueSink;
  }

  private static final class SinkNode<ResultType> extends ProcessingNode<Void> {
    public static <ResultType> void sink(BaseNode<ResultType> nodeToSink, SinkCallback<ResultType> callback) {
      new SinkNode<>(nodeToSink, callback);
    }

    private BaseNode<ResultType> nodeToSink;
    private SinkCallback<ResultType> callback;

    public SinkNode(BaseNode<ResultType> nodeToSink, SinkCallback<ResultType> callback) {
      this.nodeToSink = nodeToSink;
      this.callback = callback;
      this.callback.addRef(this);
      activate();
    }


    @Override
    public void activate() {
      nodeToSink.activate(this);
      update();
    }

    @Override
    void activate(BaseNode<?> activator) {
      throw new UnsupportedOperationException("Cannot have nodes that depend on Sink Nodes!");
    }

    @Override
    void onDependencyUpdated(BaseNode<?> dependency) {
      update();
    }

    protected void deactivate() {
      nodeToSink.deactivate(this);
    }

    private NodeResult<ResultType> lastResult = null;

    @Override
    void process() {
      NodeResult<ResultType> result = nodeToSink.getResult();

      if (lastResult != result && (lastResult == null || !lastResult.equals(result))) {
        if (result.isException()) {
          callback.onNewException(result.getException());
        } else {
          callback.onNewResult(result.getResult());
        }
      }
      lastResult = result;
    }
  }


  /**
   * Created by amlewis on 7/15/15.
   * SinkCallbacks handle extracting results out of Graphy.
   * It is important to hold a strong reference to the SinkCallback in order to prevent it's associated SinkNode from
   * getting garbage collected.
   */
  public abstract static class SinkCallback<ResultType> {
    // Keeps strong references to sink nodes so they don't get cleaned up.
    private AtomicReference<SinkNode<ResultType>> nodeRef = new AtomicReference<>(null);

    void addRef(SinkNode<ResultType> node) {
      if (!nodeRef.compareAndSet(null, node)) {
        throw new IllegalStateException("A SinkCallback object can only be set once!");
      }
    }

    protected abstract void onNewResult(ResultType result);

    protected abstract void onNewException(Exception exception);

    protected abstract void onUnset();

    public final void deregister() {
      SinkNode<ResultType> node = nodeRef.get();
      if (node != null) {
        node.deactivate();
      }
    }

    static class FutureSinkCallback<ResultType> extends SinkCallback<ResultType> implements Future<ResultType> {
      private ArrayBlockingQueue<NodeResult<ResultType>> resultQueue = new ArrayBlockingQueue<>(1);
      private AtomicBoolean cancelled = new AtomicBoolean(false);

      @Override
      public void onNewResult(ResultType result) {
        this.resultQueue.offer(new NodeResult<ResultType>(result));
      }

      @Override
      public void onNewException(Exception exception) {
        this.resultQueue.offer(new NodeResult<ResultType>(exception));
      }

      @Override
      protected void onUnset() {
        // Do nothing. Since FutureSinkCallback is used exclusively after FirstValueNode, and FirstValueNode never calls unset, we should be fine doing nothing.
      }

      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        // TODO: Support cancelling
        return false;
      }

      @Override
      public boolean isCancelled() {
        // TODO: Support cancelling
        return false;
      }

      @Override
      public boolean isDone() {
        return !resultQueue.isEmpty();
      }

      @Override
      public ResultType get() throws InterruptedException, ExecutionException {
        // TODO: Delay between take and offer may lead to a different result being placed instead or a bad call to isDone()
        NodeResult<ResultType> result = resultQueue.take();
        resultQueue.offer(result);
        if (result.isException()) {
          throw new ExecutionException(result.getException());
        }
        return result.getResult();
      }

      @Override
      public ResultType get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        // TODO: Delay between take and offer may lead to a different result being placed instead or a bad call to isDone()
        NodeResult<ResultType> result = resultQueue.take();
        if (result == null) {
          throw new TimeoutException();
        }
        resultQueue.offer(result, timeout, unit);
        if (result.isException()) {
          throw new ExecutionException(result.getException());
        }
        return result.getResult();
      }
    }
  }

  private static final class SinkLinkedBlockingDeque<ResultType> extends LinkedBlockingDeque<NodeResult<ResultType>> {
    private final QueueSinkCallback callback;

    public SinkLinkedBlockingDeque(int capacity) {
      super(capacity);
      this.callback = new QueueSinkCallback(this);
    }

    private SinkCallback<ResultType> getCallback() {
      return callback;
    }

    class QueueSinkCallback extends SinkCallback<ResultType> {
      private final BlockingQueue<NodeResult<ResultType>> queue;

      public QueueSinkCallback(BlockingQueue<NodeResult<ResultType>> queue) {
        this.queue = queue;
      }

      @Override
      public void onNewResult(ResultType result) {
        queue.offer(new NodeResult<ResultType>(result));
      }

      @Override
      public void onNewException(Exception exception) {
        queue.offer(new NodeResult<ResultType>(exception));
      }

      @Override
      protected void onUnset() {
        // Skip unsets for now. Might be good to push nulls later?
      }
    }
  }
}
