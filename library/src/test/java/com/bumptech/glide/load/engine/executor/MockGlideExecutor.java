package com.bumptech.glide.load.engine.executor;

/**
 * Creates mock {@link GlideExecutor}s.
 */
public final class MockGlideExecutor {

  private MockGlideExecutor() { }

  public static GlideExecutor newMainThreadExecutor() {
    return new GlideExecutor(1 /*poolSize*/, "mock-glide-executor",
        GlideExecutor.UncaughtThrowableStrategy.THROW, false /*preventNetworkOperations*/,
        true /*runAllOnMainThread*/);
  }

  public static GlideExecutor newMainThreadUnlimitedExecutor() {
    return new GlideExecutor(0 /* corePoolSize */,
        Integer.MAX_VALUE /* maximumPoolSize */,
        java.util.concurrent.TimeUnit.SECONDS.toMillis(10) /* keepAliveTimeInMs */,
        "mock-unlimited-glide-executor",
        GlideExecutor.UncaughtThrowableStrategy.THROW, false /*preventNetworkOperations*/,
        true /*runAllOnMainThread*/);
  }
}
