package com.github.amlewis.graphy.core;

import java.util.concurrent.*;

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

  private ExecutorService graphyExecutorService = Executors.newCachedThreadPool();

  ExecutorService getGraphyExecutorService() {
    return graphyExecutorService;
  }

  private ExecutorService defaultProcessingExecutorService = Executors.newCachedThreadPool();

  ExecutorService getDefaultProcessingExecutorService() {
    return defaultProcessingExecutorService;
  }

  public void setDefaultProcessingExecutorService(ExecutorService executorService) {
    this.defaultProcessingExecutorService = executorService;
  }

  public static <ResultType> void sink(BaseNode<ResultType> node, SinkCallback<ResultType> callback) {
    new SinkNode<>(node, callback);
  }

  public static <ResultType> Future<ResultType> singleSinkFuture(BaseNode<ResultType> node) {
    SinkCallback.FutureSinkCallback<ResultType> futureSinkCallback = new SinkCallback.FutureSinkCallback<>();
    SinkNode.sink(FirstValueNode.wrap(node), futureSinkCallback);
    return futureSinkCallback;
  }

  public static <ResultType> ResultType singleSink(BaseNode<ResultType> node) throws ExecutionException, InterruptedException {
    return singleSinkFuture(node).get();
  }

  public static <ResultType> ResultType singleSink(BaseNode<ResultType> node, long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
    return singleSinkFuture(node).get(timeout, unit);
  }
}
