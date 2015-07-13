package com.github.amlewis.graphy.core;

import java.util.Collection;

/**
 * Created by amlewis on 7/11/15.
 */
public abstract class SinkNode<ResultType> extends AbstractProcessingNode<ResultType> {
  public SinkNode(BaseNode<?>... dependencies) {
    super(dependencies);
    activate();
  }

  public SinkNode(Collection<BaseNode<?>> dependencies) {
    super(dependencies);
    activate();
  }

  @Override
  void activate(BaseNode<?> activator) {
    throw new UnsupportedOperationException("Cannot have nodes that depend on Sink Nodes!");
  }
}
