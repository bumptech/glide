package com.bumptech.glide.load.engine.executor;

import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.bumptech.glide.util.Synthetic;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/** A prioritized {@link ThreadPoolExecutor} for running jobs in Glide. */
public final class GlideExecutor implements ExecutorService {
  /**
   * The default thread name prefix for executors used to load/decode/transform data not found in
   * cache.
   */
  static final String DEFAULT_SOURCE_EXECUTOR_NAME = "source";

  /**
   * The default thread name prefix for executors used to load/decode/transform data found in
   * Glide's cache.
   */
  static final String DEFAULT_DISK_CACHE_EXECUTOR_NAME = "disk-cache";

  /**
   * The default thread count for executors used to load/decode/transform data found in Glide's
   * cache.
   */
  static final int DEFAULT_DISK_CACHE_EXECUTOR_THREADS = 1;

  private static final String TAG = "GlideExecutor";

  /**
   * The default thread name prefix for executors from unlimited thread pool used to
   * load/decode/transform data not found in cache.
   */
  private static final String DEFAULT_SOURCE_UNLIMITED_EXECUTOR_NAME = "source-unlimited";

  static final String DEFAULT_ANIMATION_EXECUTOR_NAME = "animation";

  /** The default keep alive time for threads in our cached thread pools in milliseconds. */
  private static final long KEEP_ALIVE_TIME_MS = TimeUnit.SECONDS.toMillis(10);

  // Don't use more than four threads when automatically determining thread count..
  private static final int MAXIMUM_AUTOMATIC_THREAD_COUNT = 4;

  // May be accessed on other threads, but this is an optimization only so it's ok if we set its
  // value more than once.
  private static volatile int bestThreadCount;

  private final ExecutorService delegate;

  /**
   * Returns a new {@link Builder} with the {@link #DEFAULT_DISK_CACHE_EXECUTOR_THREADS} threads,
   * {@link #DEFAULT_DISK_CACHE_EXECUTOR_NAME} name and {@link UncaughtThrowableStrategy#DEFAULT}
   * uncaught throwable strategy.
   *
   * <p>Disk cache executors do not allow network operations on their threads.
   */
  public static GlideExecutor.Builder newDiskCacheBuilder() {
    return new GlideExecutor.Builder(/* preventNetworkOperations= */ true)
        .setThreadCount(DEFAULT_DISK_CACHE_EXECUTOR_THREADS)
        .setName(DEFAULT_DISK_CACHE_EXECUTOR_NAME);
  }

  /** Shortcut for calling {@link Builder#build()} on {@link #newDiskCacheBuilder()}. */
  public static GlideExecutor newDiskCacheExecutor() {
    return newDiskCacheBuilder().build();
  }

  /**
   * @deprecated Use {@link #newDiskCacheBuilder()} and {@link
   *     Builder#setUncaughtThrowableStrategy(UncaughtThrowableStrategy)} instead.
   */
  // Public API.
  @SuppressWarnings("unused")
  @Deprecated
  public static GlideExecutor newDiskCacheExecutor(
      UncaughtThrowableStrategy uncaughtThrowableStrategy) {
    return newDiskCacheBuilder().setUncaughtThrowableStrategy(uncaughtThrowableStrategy).build();
  }

  /**
   * @deprecated Use {@link #newDiskCacheBuilder()} instead.
   */
  // Public API.
  @SuppressWarnings("WeakerAccess")
  @Deprecated
  public static GlideExecutor newDiskCacheExecutor(
      int threadCount, String name, UncaughtThrowableStrategy uncaughtThrowableStrategy) {
    return newDiskCacheBuilder()
        .setThreadCount(threadCount)
        .setName(name)
        .setUncaughtThrowableStrategy(uncaughtThrowableStrategy)
        .build();
  }

  /**
   * Returns a new {@link Builder} with the default thread count returned from {@link
   * #calculateBestThreadCount()}, the {@link #DEFAULT_SOURCE_EXECUTOR_NAME} thread name prefix, and
   * the {@link
   * com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy#DEFAULT}
   * uncaught throwable strategy.
   *
   * <p>Source executors allow network operations on their threads.
   */
  public static GlideExecutor.Builder newSourceBuilder() {
    return new GlideExecutor.Builder(/* preventNetworkOperations= */ false)
        .setThreadCount(calculateBestThreadCount())
        .setName(DEFAULT_SOURCE_EXECUTOR_NAME);
  }

  /** Shortcut for calling {@link Builder#build()} on {@link #newSourceBuilder()}. */
  public static GlideExecutor newSourceExecutor() {
    return newSourceBuilder().build();
  }

  /**
   * @deprecated Use {@link #newSourceBuilder()} instead.
   */
  // Public API.
  @SuppressWarnings("unused")
  @Deprecated
  public static GlideExecutor newSourceExecutor(
      UncaughtThrowableStrategy uncaughtThrowableStrategy) {
    return newSourceBuilder().setUncaughtThrowableStrategy(uncaughtThrowableStrategy).build();
  }

  /**
   * @deprecated Use {@link #newSourceBuilder()} instead.
   */
  // Public API.
  @SuppressWarnings("WeakerAccess")
  @Deprecated
  public static GlideExecutor newSourceExecutor(
      int threadCount, String name, UncaughtThrowableStrategy uncaughtThrowableStrategy) {
    return newSourceBuilder()
        .setThreadCount(threadCount)
        .setName(name)
        .setUncaughtThrowableStrategy(uncaughtThrowableStrategy)
        .build();
  }

  /**
   * Returns a new unlimited thread pool with zero core thread count to make sure no threads are
   * created by default, {@link #KEEP_ALIVE_TIME_MS} keep alive time, the {@link
   * #DEFAULT_SOURCE_UNLIMITED_EXECUTOR_NAME} thread name prefix, the {@link
   * com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy#DEFAULT}
   * uncaught throwable strategy, and the {@link SynchronousQueue} since using default unbounded
   * blocking queue, for example, {@link PriorityBlockingQueue} effectively won't create more than
   * {@code corePoolSize} threads. See <a href=
   * "http://developer.android.com/reference/java/util/concurrent/ThreadPoolExecutor.html">
   * ThreadPoolExecutor documentation</a>.
   *
   * <p>Source executors allow network operations on their threads.
   */
  public static GlideExecutor newUnlimitedSourceExecutor() {
    return new GlideExecutor(
        new ThreadPoolExecutor(
            0,
            Integer.MAX_VALUE,
            KEEP_ALIVE_TIME_MS,
            TimeUnit.MILLISECONDS,
            new SynchronousQueue<Runnable>(),
            new DefaultThreadFactory(
                new DefaultPriorityThreadFactory(),
                DEFAULT_SOURCE_UNLIMITED_EXECUTOR_NAME,
                UncaughtThrowableStrategy.DEFAULT,
                false)));
  }

  /**
   * Returns a new fixed thread pool that defaults to either one or two threads depending on the
   * number of available cores to use when loading frames of animations.
   *
   * <p>Animation executors do not allow network operations on their threads.
   */
  public static GlideExecutor.Builder newAnimationBuilder() {
    int maximumPoolSize = calculateAnimationExecutorThreadCount();
    return new GlideExecutor.Builder(/* preventNetworkOperations= */ true)
        .setThreadCount(maximumPoolSize)
        .setName(DEFAULT_ANIMATION_EXECUTOR_NAME);
  }

  static int calculateAnimationExecutorThreadCount() {
    int bestThreadCount = calculateBestThreadCount();
    // We don't want to add a ton of threads running animations in parallel with our source and
    // disk cache executors. Doing so adds unnecessary CPU load and can also dramatically increase
    // our maximum memory usage. Typically one thread is sufficient here, but for higher end devices
    // with more cores, two threads can provide better performance if lots of GIFs are showing at
    // once.
    return bestThreadCount >= 4 ? 2 : 1;
  }

  /** Shortcut for calling {@link Builder#build()} on {@link #newAnimationBuilder()}. */
  public static GlideExecutor newAnimationExecutor() {
    return newAnimationBuilder().build();
  }

  /**
   * @deprecated Use {@link #newAnimationBuilder()} instead.
   */
  // Public API.
  @SuppressWarnings("WeakerAccess")
  @Deprecated
  public static GlideExecutor newAnimationExecutor(
      int threadCount, UncaughtThrowableStrategy uncaughtThrowableStrategy) {
    return newAnimationBuilder()
        .setThreadCount(threadCount)
        .setUncaughtThrowableStrategy(uncaughtThrowableStrategy)
        .build();
  }

  @VisibleForTesting
  GlideExecutor(ExecutorService delegate) {
    this.delegate = delegate;
  }

  @Override
  public void execute(@NonNull Runnable command) {
    delegate.execute(command);
  }

  @NonNull
  @Override
  public Future<?> submit(@NonNull Runnable task) {
    return delegate.submit(task);
  }

  @NonNull
  @Override
  public <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> tasks)
      throws InterruptedException {
    return delegate.invokeAll(tasks);
  }

  @NonNull
  @Override
  public <T> List<Future<T>> invokeAll(
      @NonNull Collection<? extends Callable<T>> tasks, long timeout, @NonNull TimeUnit unit)
      throws InterruptedException {
    return delegate.invokeAll(tasks, timeout, unit);
  }

  @NonNull
  @Override
  public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> tasks)
      throws InterruptedException, ExecutionException {
    return delegate.invokeAny(tasks);
  }

  @Override
  public <T> T invokeAny(
      @NonNull Collection<? extends Callable<T>> tasks, long timeout, @NonNull TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return delegate.invokeAny(tasks, timeout, unit);
  }

  @NonNull
  @Override
  public <T> Future<T> submit(@NonNull Runnable task, T result) {
    return delegate.submit(task, result);
  }

  @Override
  public <T> Future<T> submit(@NonNull Callable<T> task) {
    return delegate.submit(task);
  }

  @Override
  public void shutdown() {
    delegate.shutdown();
  }

  @NonNull
  @Override
  public List<Runnable> shutdownNow() {
    return delegate.shutdownNow();
  }

  @Override
  public boolean isShutdown() {
    return delegate.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return delegate.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, @NonNull TimeUnit unit)
      throws InterruptedException {
    return delegate.awaitTermination(timeout, unit);
  }

  @Override
  public String toString() {
    return delegate.toString();
  }

  /** Determines the number of cores available on the device. */
  // Public API.
  @SuppressWarnings("WeakerAccess")
  public static int calculateBestThreadCount() {
    if (bestThreadCount == 0) {
      bestThreadCount =
          Math.min(MAXIMUM_AUTOMATIC_THREAD_COUNT, RuntimeCompat.availableProcessors());
    }
    return bestThreadCount;
  }

  /**
   * A strategy for handling unexpected and uncaught {@link Throwable}s thrown by futures run on the
   * pool.
   */
  public interface UncaughtThrowableStrategy {
    /** Silently catches and ignores the uncaught {@link Throwable}s. */
    // Public API.
    @SuppressWarnings("unused")
    UncaughtThrowableStrategy IGNORE =
        new UncaughtThrowableStrategy() {
          @Override
          public void handle(Throwable t) {
            // ignore
          }
        };
    /** Logs the uncaught {@link Throwable}s using {@link #TAG} and {@link Log}. */
    UncaughtThrowableStrategy LOG =
        new UncaughtThrowableStrategy() {
          @Override
          public void handle(Throwable t) {
            if (t != null && Log.isLoggable(TAG, Log.ERROR)) {
              Log.e(TAG, "Request threw uncaught throwable", t);
            }
          }
        };
    /** Rethrows the uncaught {@link Throwable}s to crash the app. */
    // Public API.
    @SuppressWarnings("unused")
    UncaughtThrowableStrategy THROW =
        new UncaughtThrowableStrategy() {
          @Override
          public void handle(Throwable t) {
            if (t != null) {
              throw new RuntimeException("Request threw uncaught throwable", t);
            }
          }
        };

    /** The default strategy, currently {@link #LOG}. */
    UncaughtThrowableStrategy DEFAULT = LOG;

    void handle(Throwable t);
  }

  private static final class DefaultPriorityThreadFactory implements ThreadFactory {
    private static final int DEFAULT_PRIORITY =
        android.os.Process.THREAD_PRIORITY_BACKGROUND
            + android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE;

    @Override
    public Thread newThread(@NonNull Runnable runnable) {
      return new Thread(runnable) {
        @Override
        public void run() {
          // why PMD suppression is needed: https://github.com/pmd/pmd/issues/808
          android.os.Process.setThreadPriority(DEFAULT_PRIORITY); // NOPMD AccessorMethodGeneration
          super.run();
        }
      };
    }
  }

  /**
   * A {@link java.util.concurrent.ThreadFactory} that builds threads slightly above priority {@link
   * android.os.Process#THREAD_PRIORITY_BACKGROUND}.
   */
  private static final class DefaultThreadFactory implements ThreadFactory {

    private final ThreadFactory delegate;
    private final String name;
    @Synthetic final UncaughtThrowableStrategy uncaughtThrowableStrategy;
    @Synthetic final boolean preventNetworkOperations;
    private final AtomicInteger threadNum = new AtomicInteger();

    DefaultThreadFactory(
        ThreadFactory delegate,
        String name,
        UncaughtThrowableStrategy uncaughtThrowableStrategy,
        boolean preventNetworkOperations) {
      this.delegate = delegate;
      this.name = name;
      this.uncaughtThrowableStrategy = uncaughtThrowableStrategy;
      this.preventNetworkOperations = preventNetworkOperations;
    }

    @Override
    public Thread newThread(@NonNull final Runnable runnable) {
      Thread newThread =
          delegate.newThread(
              new Runnable() {
                @Override
                public void run() {
                  if (preventNetworkOperations) {
                    StrictMode.setThreadPolicy(
                        new ThreadPolicy.Builder().detectNetwork().penaltyDeath().build());
                  }
                  try {
                    runnable.run();
                  } catch (Throwable t) {
                    uncaughtThrowableStrategy.handle(t);
                  }
                }
              });
      newThread.setName("glide-" + name + "-thread-" + threadNum.getAndIncrement());
      return newThread;
    }
  }

  /** A builder for {@link GlideExecutor}s. */
  public static final class Builder {
    /**
     * Prevents core and non-core threads from timing out ever if provided to {@link
     * #setThreadTimeoutMillis(long)}.
     */
    public static final long NO_THREAD_TIMEOUT = 0L;

    private final boolean preventNetworkOperations;

    private int corePoolSize;
    private int maximumPoolSize;

    @NonNull private ThreadFactory threadFactory = new DefaultPriorityThreadFactory();

    @NonNull
    private UncaughtThrowableStrategy uncaughtThrowableStrategy = UncaughtThrowableStrategy.DEFAULT;

    private String name;
    private long threadTimeoutMillis;

    @Synthetic
    Builder(boolean preventNetworkOperations) {
      this.preventNetworkOperations = preventNetworkOperations;
    }

    /**
     * Allows both core and non-core threads in the executor to be terminated if no tasks arrive for
     * at least the given timeout milliseconds.
     *
     * <p>Use {@link #NO_THREAD_TIMEOUT} to remove a previously set timeout.
     */
    public Builder setThreadTimeoutMillis(long threadTimeoutMillis) {
      this.threadTimeoutMillis = threadTimeoutMillis;
      return this;
    }

    /** Sets the maximum number of threads to use. */
    public Builder setThreadCount(@IntRange(from = 1) int threadCount) {
      corePoolSize = threadCount;
      maximumPoolSize = threadCount;
      return this;
    }

    /**
     * Sets the {@link ThreadFactory} responsible for creating threads and setting their priority.
     *
     * <p>Usage of this method may override other options on this builder. No guarantees are
     * provided with regards to the behavior of this method or how it interacts with other methods
     * on the builder. Use at your own risk.
     *
     * @deprecated This is an experimental method that may be removed without warning in a future
     *     version.
     */
    @Deprecated
    public Builder setThreadFactory(@NonNull ThreadFactory threadFactory) {
      this.threadFactory = threadFactory;
      return this;
    }

    /**
     * Sets the {@link UncaughtThrowableStrategy} to use for unexpected exceptions thrown by tasks
     * on {@link GlideExecutor}s built by this {@code Builder}.
     */
    public Builder setUncaughtThrowableStrategy(@NonNull UncaughtThrowableStrategy strategy) {
      this.uncaughtThrowableStrategy = strategy;
      return this;
    }

    /**
     * Sets the prefix to use for each thread name created by any {@link GlideExecutor}s built by
     * this {@code Builder}.
     */
    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    /** Builds a new {@link GlideExecutor} with any previously specified options. */
    public GlideExecutor build() {
      if (TextUtils.isEmpty(name)) {
        throw new IllegalArgumentException(
            "Name must be non-null and non-empty, but given: " + name);
      }
      ThreadPoolExecutor executor =
          new ThreadPoolExecutor(
              corePoolSize,
              maximumPoolSize,
              /* keepAliveTime= */ threadTimeoutMillis,
              TimeUnit.MILLISECONDS,
              new PriorityBlockingQueue<Runnable>(),
              new DefaultThreadFactory(
                  threadFactory, name, uncaughtThrowableStrategy, preventNetworkOperations));

      if (threadTimeoutMillis != NO_THREAD_TIMEOUT) {
        executor.allowCoreThreadTimeOut(true);
      }

      return new GlideExecutor(executor);
    }
  }
}
