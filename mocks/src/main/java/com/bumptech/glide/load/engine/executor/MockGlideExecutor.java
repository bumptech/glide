package com.bumptech.glide.load.engine.executor;

import android.os.StrictMode;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.google.common.util.concurrent.ForwardingExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/** Creates mock {@link GlideExecutor}s. */
@VisibleForTesting
public final class MockGlideExecutor {
  private MockGlideExecutor() {
    // Utility class.
  }

  /**
   * Returns a new Glide executor that delegates to the provided {@code executorService}.
   *
   * <p>The service provided will be considered managed by Glide and will be shutdown when {@link
   * com.bumptech.glide.Glide#tearDown()} is called. To use an executor that should not be torn down
   * by Glide use {@link #wrapExecutor(ExecutorService)}. If you're creating a new executor for a
   * test (e.g. an idling resource executor) then typically you should use this method.
   */
  @SuppressWarnings("WeakerAccess")
  public static GlideExecutor newTestExecutor(ExecutorService executorService) {
    return new GlideExecutor(executorService);
  }

  /**
   * Wraps an external executor in a {@link GlideExecutor}.
   *
   * <p>This should only be used when the executor used in a test is externally managed and will be
   * shutdown by the calling code or application, Glide will not shutdown the executor when {@link
   * com.bumptech.glide.Glide#tearDown()} is called. Most users should prefer {@link
   * #newTestExecutor(ExecutorService)}.
   */
  @SuppressWarnings("WeakerAccess")
  public static GlideExecutor wrapExecutor(ExecutorService executorService) {
    return new GlideExecutor(executorService, /* isDelegateExternal= */ true);
  }

  public static GlideExecutor newMainThreadExecutor() {
    return newTestExecutor(new DirectExecutorService());
  }

  /**
   * @deprecated Use {@link #newMainThreadExecutor} instead.
   */
  @Deprecated
  public static GlideExecutor newMainThreadUnlimitedExecutor() {
    return newMainThreadExecutor();
  }

  /**
   * DirectExecutorService that enforces StrictMode and converts ExecutionExceptions into
   * RuntimeExceptions.
   */
  private static final class DirectExecutorService extends ForwardingExecutorService {
    private static final StrictMode.ThreadPolicy THREAD_POLICY =
        new StrictMode.ThreadPolicy.Builder().detectNetwork().penaltyDeath().build();

    private final ExecutorService delegate;

    DirectExecutorService() {
      delegate = MoreExecutors.newDirectExecutorService();
    }

    @Override
    protected ExecutorService delegate() {
      return delegate;
    }

    @NonNull
    @Override
    public <T> Future<T> submit(@NonNull Runnable task, @NonNull T result) {
      return getUninterruptibly(super.submit(task, result));
    }

    @NonNull
    @Override
    public <T> Future<T> submit(@NonNull Callable<T> task) {
      return getUninterruptibly(super.submit(task));
    }

    @NonNull
    @Override
    public Future<?> submit(@NonNull Runnable task) {
      return getUninterruptibly(super.submit(task));
    }

    @Override
    public void execute(@NonNull final Runnable command) {
      delegate.execute(
          new Runnable() {
            @Override
            public void run() {
              StrictMode.ThreadPolicy oldPolicy = StrictMode.getThreadPolicy();
              StrictMode.setThreadPolicy(THREAD_POLICY);
              try {
                command.run();
              } finally {
                StrictMode.setThreadPolicy(oldPolicy);
              }
            }
          });
    }

    private <T> Future<T> getUninterruptibly(Future<T> future) {
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
      return future;
    }
  }
}
