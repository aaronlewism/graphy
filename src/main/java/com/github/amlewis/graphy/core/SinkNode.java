package com.github.amlewis.graphy.core;

import java.util.Collection;

/**
 * Created by amlewis on 7/11/15.
 */
public abstract class SinkNode<ResultType> extends Node<ResultType> {
  public SinkNode(Node<?>... dependencies) {
    super(dependencies);
    activate();
  }

  public SinkNode(Collection<Node<?>> dependencies) {
    super(dependencies);
    activate();
  }
}
