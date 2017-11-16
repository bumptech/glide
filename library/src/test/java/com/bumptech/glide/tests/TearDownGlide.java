package com.bumptech.glide.tests;

import android.content.Context;
import com.bumptech.glide.Glide;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.robolectric.RuntimeEnvironment;

/**
 * Clears out Glide's disk cache and the Glide singleton after every test method.
 */
public final class TearDownGlide implements TestRule {
  private static final long TIMEOUT = 500;
  private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        base.evaluate();
        tearDownGlide();
      }
    };
  }

  private void tearDownGlide() {
    final Context context = RuntimeEnvironment.application;
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      executor.submit(new Runnable() {
        @Override
        public void run() {
          Glide.get(context).clearDiskCache();
        }
      }).get(TIMEOUT, TIMEOUT_UNIT);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    }
    executor.shutdown();
    Glide.tearDown();
  }
}
