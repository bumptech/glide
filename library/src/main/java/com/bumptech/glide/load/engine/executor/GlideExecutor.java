package com.bumptech.glide.load.engine.executor;

import android.util.Log;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A prioritized {@link ThreadPoolExecutor} for running jobs in Glide.
 */
public class GlideExecutor extends ThreadPoolExecutor {
  private static final String TAG = "GlideExecutor";
  private static final String DEFAULT_NAME = "fifo-pool";
  private final UncaughtThrowableStrategy uncaughtThrowableStrategy;

  /**
   * Constructor to build a fixed thread pool with the given pool size using
   * {@link * com.bumptech.glide.load.engine.executor.GlideExecutor.DefaultThreadFactory}.
   *
   * @param poolSize The number of threads.
   */
  public GlideExecutor(int poolSize) {
    this(poolSize, UncaughtThrowableStrategy.THROW);
  }

  /**
   * Constructor to build a fixed thread pool with the given pool size using
   * {@link * com.bumptech.glide.load.engine.executor.GlideExecutor.DefaultThreadFactory}.
   *
   * @param poolSize The number of threads.
   * @param uncaughtThrowableStrategy Dictates how the pool should handle uncaught and unexpected
   *                                  throwables thrown by Futures run by the pool.
   */
  public GlideExecutor(int poolSize,
      UncaughtThrowableStrategy uncaughtThrowableStrategy) {
    this(poolSize, poolSize, 0, TimeUnit.MILLISECONDS, new DefaultThreadFactory(),
        uncaughtThrowableStrategy);
  }

  /**
   * Constructor to build a fixed thread pool with the given pool size using {@link
   * GlideExecutor.DefaultThreadFactory}.
   *
   * @param name The prefix for threads created by this pool.
   * @param poolSize The number of threads.
   */
  public GlideExecutor(String name, int poolSize) {
    this(poolSize, poolSize, 0, TimeUnit.MILLISECONDS, new DefaultThreadFactory(name),
        UncaughtThrowableStrategy.THROW);
  }

  public GlideExecutor(int corePoolSize, int maximumPoolSize, long keepAlive,
      TimeUnit timeUnit, ThreadFactory threadFactory,
      UncaughtThrowableStrategy uncaughtThrowableStrategy) {
    super(corePoolSize, maximumPoolSize, keepAlive, timeUnit, new PriorityBlockingQueue<Runnable>(),
        threadFactory);
    this.uncaughtThrowableStrategy = uncaughtThrowableStrategy;
  }

  @Override
  protected void afterExecute(Runnable r, Throwable t) {
    super.afterExecute(r, t);
    uncaughtThrowableStrategy.handle(t);
  }

  /**
   * A strategy for handling unexpected and uncaught throwables thrown by futures run on the pool.
   */
  public enum UncaughtThrowableStrategy {
    /**
     * Silently catches and ignores the uncaught throwables.
     */
    IGNORE,
    /**
     * Logs the uncaught throwables using {@link #TAG} and {@link Log}.
     */
    LOG {
      @Override
      protected void handle(Throwable t) {
        if (t != null && Log.isLoggable(TAG, Log.ERROR)) {
          Log.e(TAG, "Request threw uncaught throwable", t);
        }
      }
    },
    /**
     * Rethrows the uncaught throwables to crash the app.
     */
    THROW {
      @Override
      protected void handle(Throwable t) {
        super.handle(t);
        if (t != null) {
          throw new RuntimeException(t);
        }
      }
    };

    protected void handle(Throwable t) {
      // Ignore.
    }
  }

  /**
   * A {@link java.util.concurrent.ThreadFactory} that builds threads with priority {@link
   * android.os.Process#THREAD_PRIORITY_BACKGROUND}.
   */
  public static class DefaultThreadFactory implements ThreadFactory {
    private final String name;
    private int threadNum = 0;

    public DefaultThreadFactory() {
      this(DEFAULT_NAME);
    }

    public DefaultThreadFactory(String name) {
      this.name = name;
    }

    @Override
    public Thread newThread(Runnable runnable) {
      final Thread result = new Thread(runnable, name + "-thread-" + threadNum) {
        @Override
        public void run() {
          android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
          super.run();
        }
      };
      threadNum++;
      return result;
    }
  }
}
