package com.github.amlewis.graphy.core;


import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Created by amlewis on 7/15/15.
 */
public class GraphyTest {

  /**
   * We depend on SinkNode and ValueNode for virtually all other tests. This is a sanity check to make sure we're not
   * FUBAR.
   * @throws ExecutionException
   * @throws InterruptedException
   */
  @Test
  public void simpleValueNodeSinkTests() throws ExecutionException, InterruptedException {
    assertEquals("Graphy.sink(ValueNode.of(2)) should be 2", 2, Graphy.sinkFirstResult(ValueNode.of(2)).intValue());
    assertEquals("Graphy.sink(ValueNode.of(null)) should be null", null, Graphy.sinkFirstResult(ValueNode.of((Object) null)));

    NullPointerException exception = new NullPointerException();
    try {
      Graphy.sinkFirstResult(ValueNode.<Integer>of(exception));
      fail("Graphy.sink(ValueNode.of(NullPointerException)) should throw ExecutionException(NullPointerException)");
    } catch (ExecutionException e) {
      assertEquals("Graphy.sink(ValueNode.of(NullPointerException)) should throw ExecutionException(NullPointerException)", exception, e.getCause());
    }
  }
}
