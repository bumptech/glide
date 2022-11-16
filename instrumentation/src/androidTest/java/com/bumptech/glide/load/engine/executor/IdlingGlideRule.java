package com.bumptech.glide.load.engine.executor;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.idling.concurrent.IdlingThreadPoolExecutor;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/** Creates idling executors and registers them with espresso's {@link IdlingRegistry}. */
public final class IdlingGlideRule implements TestRule {

  private final UnaryOperator<GlideBuilder> additionalOptions;

  public static IdlingGlideRule newGlideRule(UnaryOperator<GlideBuilder> additionalOptions) {
    return new IdlingGlideRule(additionalOptions);
  }

  private IdlingGlideRule(UnaryOperator<GlideBuilder> additionalOptions) {
    this.additionalOptions = additionalOptions;
  }

  @Override
  public Statement apply(Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        IdlingRegistry idlingRegistry = IdlingRegistry.getInstance();

        IdlingThreadPoolExecutor sourceExecutor =
            newIdlingThreadPoolExecutor(
                GlideExecutor.DEFAULT_SOURCE_EXECUTOR_NAME,
                GlideExecutor.calculateBestThreadCount());
        idlingRegistry.register(sourceExecutor);
        IdlingThreadPoolExecutor diskCacheExecutor =
            newIdlingThreadPoolExecutor(
                GlideExecutor.DEFAULT_DISK_CACHE_EXECUTOR_NAME,
                /* poolSize= */ GlideExecutor.DEFAULT_DISK_CACHE_EXECUTOR_THREADS);
        idlingRegistry.register(diskCacheExecutor);
        IdlingThreadPoolExecutor animationExecutor =
            newIdlingThreadPoolExecutor(
                GlideExecutor.DEFAULT_ANIMATION_EXECUTOR_NAME,
                GlideExecutor.calculateAnimationExecutorThreadCount());
        idlingRegistry.register(animationExecutor);
        try {
          Glide.init(
              ApplicationProvider.getApplicationContext(),
              additionalOptions
                  .apply(new GlideBuilder())
                  .setSourceExecutor(new GlideExecutor(sourceExecutor))
                  .setDiskCacheExecutor(new GlideExecutor(diskCacheExecutor))
                  .setAnimationExecutor(new GlideExecutor(animationExecutor)));
          base.evaluate();
        } finally {
          idlingRegistry.unregister(sourceExecutor);
          idlingRegistry.unregister(diskCacheExecutor);
          idlingRegistry.unregister(animationExecutor);
          Glide.tearDown();
        }
      }
    };
  }

  private static IdlingThreadPoolExecutor newIdlingThreadPoolExecutor(String name, int poolSize) {
    return new IdlingThreadPoolExecutor(
        name,
        /* corePoolSize= */ poolSize,
        /* maximumPoolSize= */ poolSize,
        /* keepAliveTime= */ 1,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(),
        Thread::new);
  }
}
