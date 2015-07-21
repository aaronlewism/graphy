package com.github.amlewis.graphy.core;

/**
 * Created by amlewis on 7/20/15.
 */
public final class TransformNode<ResultType, InputType> extends StateNode<ResultType> {
  public static interface Function<ResultType, InputType> {
    ResultType apply(InputType input);
  }

  public static <ResultType, InputType> TransformNode<ResultType, InputType> of(Function<ResultType, InputType> transform, BaseNode<InputType> nodeToTransform) {
    return new TransformNode<ResultType, InputType>(transform, nodeToTransform);
  }

  private Function<ResultType, InputType> transform;
  private BaseNode<InputType> nodeToTransform;

  public TransformNode(Function<ResultType, InputType> transform, BaseNode<InputType> nodeToTransform) {
    this.transform = transform;
    this.nodeToTransform = nodeToTransform;
  }

  @Override
  protected ResultType processResult() throws Exception {
    return transform.apply(nodeToTransform.get());
  }
}
