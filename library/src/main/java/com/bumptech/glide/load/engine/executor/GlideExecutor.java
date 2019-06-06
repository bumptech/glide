package com.bumptech.glide.load.engine.executor;

import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.util.Log;
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

/** A prioritized {@link ThreadPoolExecutor} for running jobs in Glide. */
public final class GlideExecutor implements ExecutorService {
  /**
   * The default thread name prefix for executors used to load/decode/transform data not found in
   * cache.
   */
  private static final String DEFAULT_SOURCE_EXECUTOR_NAME = "source";

  /**
   * The default thread name prefix for executors used to load/decode/transform data found in
   * Glide's cache.
   */
  private static final String DEFAULT_DISK_CACHE_EXECUTOR_NAME = "disk-cache";

  /**
   * The default thread count for executors used to load/decode/transform data found in Glide's
   * cache.
   */
  private static final int DEFAULT_DISK_CACHE_EXECUTOR_THREADS = 1;

  private static final String TAG = "GlideExecutor";

  /**
   * The default thread name prefix for executors from unlimited thread pool used to
   * load/decode/transform data not found in cache.
   */
  private static final String SOURCE_UNLIMITED_EXECUTOR_NAME = "source-unlimited";

  private static final String ANIMATION_EXECUTOR_NAME = "animation";

  /** The default keep alive time for threads in our cached thread pools in milliseconds. */
  private static final long KEEP_ALIVE_TIME_MS = TimeUnit.SECONDS.toMillis(10);

  // Don't use more than four threads when automatically determining thread count..
  private static final int MAXIMUM_AUTOMATIC_THREAD_COUNT = 4;

  // May be accessed on other threads, but this is an optimization only so it's ok if we set its
  // value more than once.
  private static volatile int bestThreadCount;

  private final ExecutorService delegate;

  /**
   * Returns a new fixed thread pool with the default thread count returned from {@link
   * #calculateBestThreadCount()}, the {@link #DEFAULT_DISK_CACHE_EXECUTOR_NAME} thread name prefix,
   * and the {@link
   * com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy#DEFAULT}
   * uncaught throwable strategy.
   *
   * <p>Disk cache executors do not allow network operations on their threads.
   */
  public static GlideExecutor newDiskCacheExecutor() {
    return newDiskCacheExecutor(
        DEFAULT_DISK_CACHE_EXECUTOR_THREADS,
        DEFAULT_DISK_CACHE_EXECUTOR_NAME,
        UncaughtThrowableStrategy.DEFAULT);
  }

  /**
   * Returns a new fixed thread pool with the default thread count returned from {@link
   * #calculateBestThreadCount()}, the {@link #DEFAULT_DISK_CACHE_EXECUTOR_NAME} thread name prefix,
   * and a custom {@link
   * com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy} uncaught
   * throwable strategy.
   *
   * <p>Disk cache executors do not allow network operations on their threads.
   *
   * @param uncaughtThrowableStrategy The {@link
   *     com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy} to use to
   *     handle uncaught exceptions.
   */
  // Public API.
  @SuppressWarnings("unused")
  public static GlideExecutor newDiskCacheExecutor(
      UncaughtThrowableStrategy uncaughtThrowableStrategy) {
    return newDiskCacheExecutor(
        DEFAULT_DISK_CACHE_EXECUTOR_THREADS,
        DEFAULT_DISK_CACHE_EXECUTOR_NAME,
        uncaughtThrowableStrategy);
  }

  /**
   * Returns a new fixed thread pool with the given thread count, thread name prefix, and {@link
   * com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy}.
   *
   * <p>Disk cache executors do not allow network operations on their threads.
   *
   * @param threadCount The number of threads.
   * @param name The prefix for each thread name.
   * @param uncaughtThrowableStrategy The {@link
   *     com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy} to use to
   *     handle uncaught exceptions.
   */
  // Public API.
  @SuppressWarnings("WeakerAccess")
  public static GlideExecutor newDiskCacheExecutor(
      int threadCount, String name, UncaughtThrowableStrategy uncaughtThrowableStrategy) {
    return new GlideExecutor(
        new ThreadPoolExecutor(
            threadCount /* corePoolSize */,
            threadCount /* maximumPoolSize */,
            0 /* keepAliveTime */,
            TimeUnit.MILLISECONDS,
            new PriorityBlockingQueue<Runnable>(),
            new DefaultThreadFactory(name, uncaughtThrowableStrategy, true)));
  }

  /**
   * Returns a new fixed thread pool with the default thread count returned from {@link
   * #calculateBestThreadCount()}, the {@link #DEFAULT_SOURCE_EXECUTOR_NAME} thread name prefix, and
   * the {@link
   * com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy#DEFAULT}
   * uncaught throwable strategy.
   *
   * <p>Source executors allow network operations on their threads.
   */
  public static GlideExecutor newSourceExecutor() {
    return newSourceExecutor(
        calculateBestThreadCount(),
        DEFAULT_SOURCE_EXECUTOR_NAME,
        UncaughtThrowableStrategy.DEFAULT);
  }

  /**
   * Returns a new fixed thread pool with the default thread count returned from {@link
   * #calculateBestThreadCount()}, the {@link #DEFAULT_SOURCE_EXECUTOR_NAME} thread name prefix, and
   * a custom {@link
   * com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy} uncaught
   * throwable strategy.
   *
   * <p>Source executors allow network operations on their threads.
   *
   * @param uncaughtThrowableStrategy The {@link
   *     com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy} to use to
   *     handle uncaught exceptions.
   */
  // Public API.
  @SuppressWarnings("unused")
  public static GlideExecutor newSourceExecutor(
      UncaughtThrowableStrategy uncaughtThrowableStrategy) {
    return newSourceExecutor(
        calculateBestThreadCount(), DEFAULT_SOURCE_EXECUTOR_NAME, uncaughtThrowableStrategy);
  }

  /**
   * Returns a new fixed thread pool with the given thread count, thread name prefix, and {@link
   * com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy}.
   *
   * <p>Source executors allow network operations on their threads.
   *
   * @param threadCount The number of threads.
   * @param name The prefix for each thread name.
   * @param uncaughtThrowableStrategy The {@link
   *     com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy} to use to
   *     handle uncaught exceptions.
   */
  // Public API.
  @SuppressWarnings("WeakerAccess")
  public static GlideExecutor newSourceExecutor(
      int threadCount, String name, UncaughtThrowableStrategy uncaughtThrowableStrategy) {
    return new GlideExecutor(
        new ThreadPoolExecutor(
            threadCount /* corePoolSize */,
            threadCount /* maximumPoolSize */,
            0 /* keepAliveTime */,
            TimeUnit.MILLISECONDS,
            new PriorityBlockingQueue<Runnable>(),
            new DefaultThreadFactory(name, uncaughtThrowableStrategy, false)));
  }

  /**
   * Returns a new unlimited thread pool with zero core thread count to make sure no threads are
   * created by default, {@link #KEEP_ALIVE_TIME_MS} keep alive time, the {@link
   * #SOURCE_UNLIMITED_EXECUTOR_NAME} thread name prefix, the {@link
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
                SOURCE_UNLIMITED_EXECUTOR_NAME, UncaughtThrowableStrategy.DEFAULT, false)));
  }

  /**
   * Returns a new cached thread pool that defaults to either one or two threads depending on the
   * number of available cores to use when loading frames of animations.
   */
  public static GlideExecutor newAnimationExecutor() {
    int bestThreadCount = calculateBestThreadCount();
    // We don't want to add a ton of threads running animations in parallel with our source and
    // disk cache executors. Doing so adds unnecessary CPU load and can also dramatically increase
    // our maximum memory usage. Typically one thread is sufficient here, but for higher end devices
    // with more cores, two threads can provide better performance if lots of GIFs are showing at
    // once.
    int maximumPoolSize = bestThreadCount >= 4 ? 2 : 1;

    return newAnimationExecutor(maximumPoolSize, UncaughtThrowableStrategy.DEFAULT);
  }

  /**
   * Returns a new cached thread pool with the given thread count and {@link
   * UncaughtThrowableStrategy} to use when loading frames of animations.
   */
  // Public API.
  @SuppressWarnings("WeakerAccess")
  public static GlideExecutor newAnimationExecutor(
      int threadCount, UncaughtThrowableStrategy uncaughtThrowableStrategy) {
    return new GlideExecutor(
        new ThreadPoolExecutor(
            0 /* corePoolSize */,
            threadCount,
            KEEP_ALIVE_TIME_MS,
            TimeUnit.MILLISECONDS,
            new PriorityBlockingQueue<Runnable>(),
            new DefaultThreadFactory(ANIMATION_EXECUTOR_NAME, uncaughtThrowableStrategy, true)));
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

  /**
   * A {@link java.util.concurrent.ThreadFactory} that builds threads slightly above priority {@link
   * android.os.Process#THREAD_PRIORITY_BACKGROUND}.
   */
  private static final class DefaultThreadFactory implements ThreadFactory {
    private static final int DEFAULT_PRIORITY =
        android.os.Process.THREAD_PRIORITY_BACKGROUND
            + android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE;

    private final String name;
    @Synthetic final UncaughtThrowableStrategy uncaughtThrowableStrategy;
    @Synthetic final boolean preventNetworkOperations;
    private int threadNum;

    DefaultThreadFactory(
        String name,
        UncaughtThrowableStrategy uncaughtThrowableStrategy,
        boolean preventNetworkOperations) {
      this.name = name;
      this.uncaughtThrowableStrategy = uncaughtThrowableStrategy;
      this.preventNetworkOperations = preventNetworkOperations;
    }

    @Override
    public synchronized Thread newThread(@NonNull Runnable runnable) {
      final Thread result =
          new Thread(runnable, "glide-" + name + "-thread-" + threadNum) {
            @Override
            public void run() {
              // why PMD suppression is needed: https://github.com/pmd/pmd/issues/808
              android.os.Process.setThreadPriority(
                  DEFAULT_PRIORITY); // NOPMD AccessorMethodGeneration
              if (preventNetworkOperations) {
                StrictMode.setThreadPolicy(
                    new ThreadPolicy.Builder().detectNetwork().penaltyDeath().build());
              }
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
