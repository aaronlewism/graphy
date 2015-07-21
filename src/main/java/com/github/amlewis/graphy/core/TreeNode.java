package com.github.amlewis.graphy.core;

/**
 * Created by amlewis on 7/15/15.
 * <p/>
 * Lightweight Node to wrap a tree of nodes as a single node. If you want to create a re-usuable graph where you only
 * alter a couple of leaf dependencies, you should use this node to wrap your graph structure.
 */
public abstract class TreeNode<ResultType> extends Node<ResultType> {
  private Node<ResultType> root;

  public TreeNode(Node<ResultType> root) {
    this.root = root;
  }

  @Override
  protected void activate() {
    root.activate(this);
  }

  @Override
  void onDependencyUpdated(Node<?> dependency) {
    setResult(root.getResult());
  }
}
