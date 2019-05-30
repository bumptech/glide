package com.bumptech.glide.test;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/** Clears out Glide's disk cache and the Glide singleton after every test method. */
public final class TearDownGlide implements TestRule {

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          base.evaluate();
        } finally {
          new ConcurrencyHelper()
              .runOnMainThread(
                  new Runnable() {
                    @Override
                    public void run() {
                      // Casting to Context explicitly is required on Java8, or the context will
                      // be interpreted as a FragmentActivity.
                      RequestManager requestManager =
                          Glide.with(ApplicationProvider.<Context>getApplicationContext());
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
