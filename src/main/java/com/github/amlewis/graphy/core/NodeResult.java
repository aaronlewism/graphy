package com.github.amlewis.graphy.core;

/**
 * Created by amlewis on 7/12/15.
 */
class NodeResult<ResultType> {
  private final ResultType result;
  private final Exception exception;

  public NodeResult(ResultType result) {
    this.result = result;
    this.exception = null;
  }

  public NodeResult(Exception exception) {
    if (exception == null) {
      throw new IllegalArgumentException("Exception cannot be null!");
    }
    this.result = null;
    this.exception = exception;
  }

  public ResultType getResult() {
    return result;
  }

  public Exception getException() {
    return exception;
  }

  public boolean isException() {
    return exception != null;
  }
}
