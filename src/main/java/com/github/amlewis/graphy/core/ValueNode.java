package com.github.amlewis.graphy.core;

/**
 * Created by amlewis on 7/10/15.
 */
public class ValueNode<ValueType> extends BaseNode<ValueType> {
  public static <ResultType> ValueNode<ResultType> of(ResultType value) {
    return new ValueNode<ResultType>(value);
  }

  public static <ResultType> ValueNode<ResultType> of(Exception exception) {
    return new ValueNode<ResultType>(exception);
  }

  public ValueNode(ValueType result) {
    super();
    setResult(result);
  }

  public ValueNode(Exception exception) {
    super();
    setResult(exception);
  }

  public void setValue(ValueType result) {
    setResult(result);
  }

  public void setValue(Exception exception) {
    setResult(exception);
  }

  public void unset() {
    setResult((NodeResult<ValueType>) null);
  }

  @Override
  public void activate() {
    // No activation needed
  }

  @Override
  void onDependencyUpdated(BaseNode<?> dependency) {
    // No dependencies. Probably won't ever be called?
  }
}
