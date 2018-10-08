package com.bumptech.glide.test;

import androidx.test.InstrumentationRegistry;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Clears out Glide's disk cache and the Glide singleton after every test method.
 */
public final class TearDownGlide implements TestRule {

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          base.evaluate();
        } finally {
          new ConcurrencyHelper().runOnMainThread(new Runnable() {
            @Override
            public void run() {
              RequestManager requestManager =
                  Glide.with(InstrumentationRegistry.getTargetContext());
              requestManager.onStop();
              requestManager.onDestroy();
            }
          });
          Glide.tearDown();
        }
      }
    };
  }
}
