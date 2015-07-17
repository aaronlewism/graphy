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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    NodeResult<ResultType> that = (NodeResult<ResultType>) o;

    if (result != null ? !result.equals(that.result) : that.result != null) return false;
    return !(exception != null ? !exception.equals(that.exception) : that.exception != null);
  }

  @Override
  public int hashCode() {
    int result1 = result != null ? result.hashCode() : 0;
    result1 = 31 * result1 + (exception != null ? exception.hashCode() : 0);
    return result1;
  }
}
