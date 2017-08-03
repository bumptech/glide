package com.bumptech.glide.load.engine.executor;

import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.support.annotation.NonNull;
import android.util.Log;
import com.bumptech.glide.util.Synthetic;
import java.io.File;
import java.io.FilenameFilter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * A prioritized {@link ThreadPoolExecutor} for running jobs in Glide.
 */
public final class GlideExecutor extends ThreadPoolExecutor {

  /**
   * The default thread name prefix for executors used to load/decode/transform data not found in
   * cache.
   */
  public static final String DEFAULT_SOURCE_EXECUTOR_NAME = "source";
  /**
   * The default thread name prefix for executors used to load/decode/transform data found in
   * Glide's cache.
   */
  public static final String DEFAULT_DISK_CACHE_EXECUTOR_NAME = "disk-cache";
  /**
   * The default thread count for executors used to load/decode/transform data found in Glide's
   * cache.
   */
  public static final int DEFAULT_DISK_CACHE_EXECUTOR_THREADS = 1;

  private static final String TAG = "GlideExecutor";
  private static final String CPU_NAME_REGEX = "cpu[0-9]+";
  private static final String CPU_LOCATION = "/sys/devices/system/cpu/";
  // Don't use more than four threads when automatically determining thread count..
  private static final int MAXIMUM_AUTOMATIC_THREAD_COUNT = 4;
  private final boolean executeSynchronously;

  /**
   * The default thread name prefix for executors from unlimited thread pool used to
   * load/decode/transform data not found in cache.
   */
  private static final String SOURCE_UNLIMITED_EXECUTOR_NAME = "source-unlimited";
  /**
   * The default keep alive time for threads in source unlimited executor pool in milliseconds.
   */
  private static final long SOURCE_UNLIMITED_EXECUTOR_KEEP_ALIVE_TIME_MS =
      TimeUnit.SECONDS.toMillis(10);

  /**
   * Returns a new fixed thread pool with the default thread count returned from
   * {@link #calculateBestThreadCount()}, the {@link #DEFAULT_DISK_CACHE_EXECUTOR_NAME} thread name
   * prefix, and the
   * {@link com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy#DEFAULT}
   * uncaught throwable strategy.
   *
   * <p>Disk cache executors do not allow network operations on their threads.
   */
  public static GlideExecutor newDiskCacheExecutor() {
    return newDiskCacheExecutor(DEFAULT_DISK_CACHE_EXECUTOR_THREADS,
        DEFAULT_DISK_CACHE_EXECUTOR_NAME, UncaughtThrowableStrategy.DEFAULT);
  }

  /**
   * Returns a new fixed thread pool with the given thread count, thread name prefix,
   * and {@link com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy}.
   *
   * <p>Disk cache executors do not allow network operations on their threads.
   *
   * @param threadCount The number of threads.
   * @param name The prefix for each thread name.
   * @param uncaughtThrowableStrategy The {@link
   * com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy} to use to
   *                                  handle uncaught exceptions.
   */
  public static GlideExecutor newDiskCacheExecutor(int threadCount, String name,
      UncaughtThrowableStrategy uncaughtThrowableStrategy) {
    return new GlideExecutor(threadCount, name, uncaughtThrowableStrategy,
        true /*preventNetworkOperations*/, false /*executeSynchronously*/);
  }

  /**
   * Returns a new fixed thread pool with the default thread count returned from
   * {@link #calculateBestThreadCount()}, the {@link #DEFAULT_SOURCE_EXECUTOR_NAME} thread name
   * prefix, and the
   * {@link com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy#DEFAULT}
   * uncaught throwable strategy.
   *
   * <p>Source executors allow network operations on their threads.
   */
  public static GlideExecutor newSourceExecutor() {
    return newSourceExecutor(calculateBestThreadCount(), DEFAULT_SOURCE_EXECUTOR_NAME,
        UncaughtThrowableStrategy.DEFAULT);
  }

  /**
   * Returns a new fixed thread pool with the given thread count, thread name prefix,
   * and {@link com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy}.
   *
   * <p>Source executors allow network operations on their threads.
   *
   * @param threadCount The number of threads.
   * @param name The prefix for each thread name.
   * @param uncaughtThrowableStrategy The {@link
   * com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy} to use to
   *                                  handle uncaught exceptions.
   */
  public static GlideExecutor newSourceExecutor(int threadCount, String name,
      UncaughtThrowableStrategy uncaughtThrowableStrategy) {
    return new GlideExecutor(threadCount, name, uncaughtThrowableStrategy,
        false /*preventNetworkOperations*/, false /*executeSynchronously*/);
  }

  /**
   * Returns a new unlimited thread pool with zero core thread count to make sure no threads are
   * created by default, {@link #SOURCE_UNLIMITED_EXECUTOR_KEEP_ALIVE_TIME_MS} keep alive
   * time, the {@link #SOURCE_UNLIMITED_EXECUTOR_NAME} thread name prefix, the
   * {@link com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy#DEFAULT}
   * uncaught throwable strategy, and the {@link SynchronousQueue} since using default unbounded
   * blocking queue, for example, {@link PriorityBlockingQueue} effectively won't create more than
   * {@code corePoolSize} threads.
   * See <a href=
   * "http://developer.android.com/reference/java/util/concurrent/ThreadPoolExecutor.html">
   * ThreadPoolExecutor documentation</a>.
   *
   * <p>Source executors allow network operations on their threads.
   */
  public static GlideExecutor newUnlimitedSourceExecutor() {
    return new GlideExecutor(0 /* corePoolSize */,
        Integer.MAX_VALUE /* maximumPoolSize */,
        SOURCE_UNLIMITED_EXECUTOR_KEEP_ALIVE_TIME_MS,
        SOURCE_UNLIMITED_EXECUTOR_NAME,
        UncaughtThrowableStrategy.DEFAULT,
        false /*preventNetworkOperations*/,
        false /*executeSynchronously*/,
        new SynchronousQueue<Runnable>());
  }

  // Visible for testing.
  GlideExecutor(int poolSize, String name,
      UncaughtThrowableStrategy uncaughtThrowableStrategy, boolean preventNetworkOperations,
      boolean executeSynchronously) {
    this(
        poolSize /* corePoolSize */,
        poolSize /* maximumPoolSize */,
        0 /* keepAliveTimeInMs */,
        name,
        uncaughtThrowableStrategy,
        preventNetworkOperations,
        executeSynchronously);
  }

  GlideExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTimeInMs, String name,
      UncaughtThrowableStrategy uncaughtThrowableStrategy, boolean preventNetworkOperations,
      boolean executeSynchronously) {
    this(
        corePoolSize,
        maximumPoolSize,
        keepAliveTimeInMs,
        name,
        uncaughtThrowableStrategy,
        preventNetworkOperations,
        executeSynchronously,
        new PriorityBlockingQueue<Runnable>());
  }

  GlideExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTimeInMs, String name,
      UncaughtThrowableStrategy uncaughtThrowableStrategy, boolean preventNetworkOperations,
      boolean executeSynchronously, BlockingQueue<Runnable> queue) {
    super(
        corePoolSize,
        maximumPoolSize,
        keepAliveTimeInMs,
        TimeUnit.MILLISECONDS,
        queue,
        new DefaultThreadFactory(name, uncaughtThrowableStrategy, preventNetworkOperations));
    this.executeSynchronously = executeSynchronously;
  }

  @Override
  public void execute(Runnable command) {
    if (executeSynchronously) {
      command.run();
    } else {
      super.execute(command);
    }
  }

  @NonNull
  @Override
  public Future<?> submit(Runnable task) {
    return maybeWait(super.submit(task));
  }

  private <T> Future<T> maybeWait(Future<T> future) {
    if (executeSynchronously) {
      boolean interrupted = false;
      try {
        while (!future.isDone()) {
          try {
            future.get();
          } catch (ExecutionException e) {
            throw new RuntimeException(e);
          } catch (InterruptedException e) {
            interrupted = true;
          }
        }
      } finally {
        if (interrupted) {
          Thread.currentThread().interrupt();
        }
      }
    }
    return future;
  }

  @NonNull
  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    return maybeWait(super.submit(task, result));
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    return maybeWait(super.submit(task));
  }

  /**
   * Determines the number of cores available on the device.
   *
   * <p>{@link Runtime#availableProcessors()} returns the number of awake cores, which may not
   * be the number of available cores depending on the device's current state. See
   * http://goo.gl/8H670N.
   */
  public static int calculateBestThreadCount() {
    // We override the current ThreadPolicy to allow disk reads.
    // This shouldn't actually do disk-IO and accesses a device file.
    // See: https://github.com/bumptech/glide/issues/1170
    ThreadPolicy originalPolicy = StrictMode.allowThreadDiskReads();
    File[] cpus = null;
    try {
      File cpuInfo = new File(CPU_LOCATION);
      final Pattern cpuNamePattern = Pattern.compile(CPU_NAME_REGEX);
      cpus = cpuInfo.listFiles(new FilenameFilter() {
        @Override
        public boolean accept(File file, String s) {
          return cpuNamePattern.matcher(s).matches();
        }
      });
    } catch (Throwable t) {
      if (Log.isLoggable(TAG, Log.ERROR)) {
        Log.e(TAG, "Failed to calculate accurate cpu count", t);
      }
    } finally {
      StrictMode.setThreadPolicy(originalPolicy);
    }

    int cpuCount = cpus != null ? cpus.length : 0;
    int availableProcessors = Math.max(1, Runtime.getRuntime().availableProcessors());
    return Math.min(MAXIMUM_AUTOMATIC_THREAD_COUNT, Math.max(availableProcessors, cpuCount));
  }

  /**
   * A strategy for handling unexpected and uncaught {@link Throwable}s thrown by futures run on the
   * pool.
   */
  public enum UncaughtThrowableStrategy {
    /**
     * Silently catches and ignores the uncaught {@link Throwable}s.
     */
    IGNORE,
    /**
     * Logs the uncaught {@link Throwable}s using {@link #TAG} and {@link Log}.
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
     * Rethrows the uncaught {@link Throwable}s to crash the app.
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

    /** The default strategy, currently {@link #LOG}. */
    public static final UncaughtThrowableStrategy DEFAULT = LOG;

    protected void handle(Throwable t) {
      // Ignore.
    }
  }

  /**
   * A {@link java.util.concurrent.ThreadFactory} that builds threads slightly above priority {@link
   * android.os.Process#THREAD_PRIORITY_BACKGROUND}.
   */
  private static final class DefaultThreadFactory implements ThreadFactory {
    private final String name;
    @Synthetic final UncaughtThrowableStrategy uncaughtThrowableStrategy;
    @Synthetic final boolean preventNetworkOperations;
    private int threadNum;

    DefaultThreadFactory(String name, UncaughtThrowableStrategy uncaughtThrowableStrategy,
        boolean preventNetworkOperations) {
      this.name = name;
      this.uncaughtThrowableStrategy = uncaughtThrowableStrategy;
      this.preventNetworkOperations = preventNetworkOperations;
    }

    @Override
    public synchronized Thread newThread(@NonNull Runnable runnable) {
      final Thread result = new Thread(runnable, "glide-" + name + "-thread-" + threadNum) {
        @Override
        public void run() {
          android.os.Process.setThreadPriority(
              android.os.Process.THREAD_PRIORITY_BACKGROUND
              + android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE);
          if (preventNetworkOperations) {
            StrictMode.setThreadPolicy(
                new ThreadPolicy.Builder()
                    .detectNetwork()
                    .penaltyDeath()
                    .build());
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
