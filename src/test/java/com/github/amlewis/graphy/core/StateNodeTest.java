package com.github.amlewis.graphy.core;

import org.junit.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by amlewis on 7/20/15.
 */
public class StateNodeTest {
  private static class AddNode extends StateNode<Integer> {
    private final BaseNode<Integer> left;
    private final BaseNode<Integer> right;

    public AddNode(BaseNode<Integer> left, BaseNode<Integer> right) {
      super(left, right);
      this.left = left;
      this.right = right;
    }

    @Override
    protected Integer processResult() throws Exception {
      return left.get() + right.get();
    }
  }

  @Test
  public void simpleAddNodeTests() throws InterruptedException {
    ValueNode<Integer> left = new ValueNode<Integer>();
    ValueNode<Integer> right = new ValueNode<Integer>();
    AddNode sum = new AddNode(left, right);

    BlockingQueue<NodeResult<Integer>> queue = Graphy.sinkToBlockingDeque(sum, 5);

    left.setValue(1);
    right.setValue(3);
    assertEquals(4, queue.take().getResult().intValue());

    left.setValue(5);
    assertEquals(8, queue.take().getResult().intValue());

    Exception exception = new Exception();
    right.setValue(exception);
    assertEquals(exception, queue.take().getException());

    right.setValue(1);
    assertEquals(6, queue.take().getResult().intValue());
    assertTrue(queue.isEmpty());
  }

  @Test
  public void fibonacci() throws ExecutionException, InterruptedException {
    ValueNode<Integer> first = ValueNode.of(0);
    ValueNode<Integer> second = ValueNode.of(1);

    final int FIB_NUMBER = 20;
    AddNode sum = null;
    BaseNode<Integer> minus2 = first;
    BaseNode<Integer> minus1 = second;
    for (int i = 1; i < FIB_NUMBER; ++i) {
      sum = new AddNode(minus1, minus2);
      minus2 = minus1;
      minus1 = sum;
    }

    BlockingQueue<NodeResult<Integer>> queue = Graphy.sinkToBlockingDeque(sum, 5);

    assertEquals(6765, queue.take().getResult().intValue());

    first.setValue(1);
    NodeResult<Integer> result;
    while ((result = queue.poll(1000, TimeUnit.MILLISECONDS)) != null) {
      if (result.getResult() == 10946) {
        break;
      }
    }
    assertEquals(10946, result.getResult().intValue());

    assertTrue(queue.isEmpty());
  }
}
