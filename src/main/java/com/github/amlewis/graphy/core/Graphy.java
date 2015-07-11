package com.github.amlewis.graphy.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by amlewis on 7/11/15.
 */
public class Graphy {

  private static Graphy instance;

  public static Graphy getInstance() {
    if (instance == null) {
      synchronized (Graphy.class) {
        if (instance == null) {
          instance = new Graphy();
        }
      }
    }

    return instance;
  }

  private ExecutorService executorService = Executors.newFixedThreadPool(8);

  ExecutorService getExecutorService() {
    return executorService;
  }
}
