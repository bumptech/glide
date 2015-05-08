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

  /**
   * Constructor to build a fixed thread pool with the given pool size.
   *
   * @param poolSize The number of threads.
   */
  public GlideExecutor(int poolSize) {
    this(poolSize, new DefaultThreadFactory());
  }

  /**
   * Constructor to build a fixed thread pool with the given pool size.
   *
   * @param poolSize The number of threads.
   * @param uncaughtThrowableStrategy Dictates how the pool should handle uncaught and unexpected
   *                                  throwables thrown by Futures run by the pool.
   */
  public GlideExecutor(int poolSize,
      UncaughtThrowableStrategy uncaughtThrowableStrategy) {
    this(poolSize, new DefaultThreadFactory(uncaughtThrowableStrategy));
  }

  /**
   * Constructor to build a fixed thread pool with the given pool size.
   *
   * @param name The prefix for threads created by this pool.
   * @param poolSize The number of threads.
   */
  public GlideExecutor(String name, int poolSize) {
    this(poolSize, new DefaultThreadFactory(name));
  }

  /**
   * Constructor to build a fixed thread pool with the given pool size.
   *
   * @param name The prefix for each thread name.
   * @param poolSize The number of threads.
   * @param uncaughtThrowableStrategy The {@link
   * com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy} to use to
   *                                  handle uncaught exceptions.
   */
  public GlideExecutor(String name, int poolSize,
      UncaughtThrowableStrategy uncaughtThrowableStrategy) {
    this(poolSize, new DefaultThreadFactory(name, uncaughtThrowableStrategy));
  }

  private GlideExecutor(int corePoolSize, ThreadFactory threadFactory) {
    super(corePoolSize, corePoolSize, 0, TimeUnit.MILLISECONDS,
        new PriorityBlockingQueue<Runnable>(), threadFactory);
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
          throw new RuntimeException("Request threw uncaught throwable", t);
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
  private static final class DefaultThreadFactory implements ThreadFactory {
    private final String name;
    private final UncaughtThrowableStrategy uncaughtThrowableStrategy;
    private int threadNum = 0;

    DefaultThreadFactory() {
      this(DEFAULT_NAME);
    }

    DefaultThreadFactory(String name) {
      this(name, UncaughtThrowableStrategy.LOG);
    }

    DefaultThreadFactory(UncaughtThrowableStrategy uncaughtThrowableStrategy) {
      this(DEFAULT_NAME, uncaughtThrowableStrategy);
    }

    DefaultThreadFactory(String name, UncaughtThrowableStrategy uncaughtThrowableStrategy) {
      this.name = name;
      this.uncaughtThrowableStrategy = uncaughtThrowableStrategy;
    }

    @Override
    public Thread newThread(Runnable runnable) {
      final Thread result = new Thread(runnable, name + "-thread-" + threadNum) {
        @Override
        public void run() {
          android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
          try {
            super.run();
          } catch (Throwable t) {
            uncaughtThrowableStrategy.handle(t);
          }
        }
      };
      threadNum++;
      return result;
    }
  }
}
