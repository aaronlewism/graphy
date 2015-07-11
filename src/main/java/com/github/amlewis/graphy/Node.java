package com.github.amlewis.graphy;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by amlewis on 7/10/15.
 */
public class Node<ResultType> {
  private final List<Node<?>> dependencies = new LinkedList<>();

  public Node(Node<?>... dependencies) {
    for (Node<?> node : dependencies) {
      this.dependencies.add(node);
    }
  }

  public Node(Collection<Node<?>> dependencies) {
    this.dependencies.addAll(dependencies);
  }
}
