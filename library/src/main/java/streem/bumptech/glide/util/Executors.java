package com.bumptech.glide.util;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/** Generic {@link Executor} implementations. */
public final class Executors {
  private Executors() {
    // Utility class.
  }

  private static final Executor MAIN_THREAD_EXECUTOR =
      new Executor() {
        @Override
        public void execute(@NonNull Runnable command) {
          Util.postOnUiThread(command);
        }
      };
  private static final Executor DIRECT_EXECUTOR =
      new Executor() {
        @Override
        public void execute(@NonNull Runnable command) {
          command.run();
        }
      };

  /** Posts executions to the main thread. */
  public static Executor mainThreadExecutor() {
    return MAIN_THREAD_EXECUTOR;
  }

  /** Immediately calls {@link Runnable#run()} on the current thread. */
  public static Executor directExecutor() {
    return DIRECT_EXECUTOR;
  }

  @VisibleForTesting
  public static void shutdownAndAwaitTermination(ExecutorService pool) {
    long shutdownSeconds = 5;
    pool.shutdownNow();
    try {
      if (!pool.awaitTermination(shutdownSeconds, TimeUnit.SECONDS)) {
        pool.shutdownNow();
        if (!pool.awaitTermination(shutdownSeconds, TimeUnit.SECONDS)) {
          throw new RuntimeException("Failed to shutdown");
        }
      }
    } catch (InterruptedException ie) {
      pool.shutdownNow();
      Thread.currentThread().interrupt();
      throw new RuntimeException(ie);
    }
  }
}
