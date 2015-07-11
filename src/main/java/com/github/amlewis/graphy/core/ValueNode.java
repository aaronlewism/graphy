package com.github.amlewis.graphy.core;

/**
 * Created by amlewis on 7/10/15.
 */
public class ValueNode<ResultType> extends Node<ResultType> {
  public ValueNode(ResultType result) {
    super();
    setResult(result);
  }

  public ValueNode(Exception exception) {
    super();
    setResult(exception);
  }

  @Override
  protected ResultType process() throws Exception {
    throw new UnsupportedOperationException("ValueNode's \"process\" should never be called!.");
  }

  @Override
  void update() {
    // Do nothing.
  }

  public void setValue(ResultType result) {
    setResult(result);
  }

  public void setValue(Exception exception) {
    setResult(exception);
  }
}
