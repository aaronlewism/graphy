package com.github.amlewis.graphy.core;

import org.junit.Test;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.*;

/**
 * Created by amlewis on 7/16/15.
 */
public class ValueNodeTest {
  @Test
  public void setValueBeforeSink() throws ExecutionException, InterruptedException {
    final ValueNode<Integer> valueNode = new ValueNode<Integer>();
    valueNode.setValue(5);
    assertEquals(5, Graphy.singleSink(valueNode).intValue());
  }

  @Test
  public void setValueAfterSink() throws ExecutionException, InterruptedException {
    final ValueNode<Integer> valueNode = new ValueNode<Integer>();
    final Future<Integer> sinkFuture = Graphy.singleSinkFuture(valueNode);
    valueNode.setValue(5);
    assertEquals(5, sinkFuture.get().intValue());
  }

  @Test
  public void valueNodeUpdatesShouldBeOrdered() {
    final int NUM_SETS = 1000;
    final ValueNode<Integer> valueNode = new ValueNode<Integer>();
    BlockingDeque<NodeResult<Integer>> sink = GraphyTestUtils.sinkToQueue(valueNode, NUM_SETS);

    for (int i = 1; i <= NUM_SETS; ++i) {
      valueNode.setValue(i);
    }

    for (int i=0; i<10; ++i) {
      if (sink.peekLast() == null || sink.peekLast().getResult() == null || sink.peekLast().getResult().intValue() != NUM_SETS) {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      } else {
        break;
      }
    }

    if (sink.peekLast().getResult() == null || sink.peekLast().getResult().intValue() != NUM_SETS) {
      fail("Final value of NUM_SETS is not last message in Queue after 1 second!");
    }

    Integer prev = null;
    for (NodeResult<Integer> result : sink) {
      assertNotNull("NodeResult must not be null!", result);
      assertNotNull("NodeResult.getResult must not be null!", result.getResult());
      Integer current = result.getResult();
      System.out.print(current.intValue() + " ");
      if (prev != null) {
        assertTrue("Previous Result should be less than the Current!", prev.intValue() < current.intValue());
      }
      prev = current;
    }
  }
}
