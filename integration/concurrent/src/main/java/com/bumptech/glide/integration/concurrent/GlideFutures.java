package com.bumptech.glide.integration.concurrent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.concurrent.futures.CallbackToFutureAdapter.Completer;
import androidx.concurrent.futures.CallbackToFutureAdapter.Resolver;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.util.Executors;
import com.google.common.base.Function;
import com.google.common.util.concurrent.ClosingFuture;
import com.google.common.util.concurrent.ClosingFuture.ClosingFunction;
import com.google.common.util.concurrent.ClosingFuture.DeferredCloser;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.Closeable;
import java.util.concurrent.Executor;

/** Utilities for getting ListenableFutures out of Glide. */
public final class GlideFutures {

  /**
   * Submits the provided request and returns a {@link ClosingFuture} that can be used to perform
   * intermediate operations and then release Glide's resources.
   *
   * <p>Typical usage of this is primarily intended for operating on Bitmaps and returning them to
   * the Glide pool when done.
   *
   * <p>An example usage:
   *
   * <pre>{@code
   * ListenableFuture<?> future =
   *     submit(
   *       imageManager,
   *       requestBuilder,
   *       backgroundExecutor)
   *     .transform((closer, bitmap) -> doSomethingWithBitmap(bitmap), backgroundExecutor)
   *     .closing()
   * ;
   * }</pre>
   */
  public static <T> ClosingFuture<T> submit(
      RequestManager requestManager, RequestBuilder<T> requestBuilder, Executor executor) {
    return ClosingFuture.from(submitInternal(requestBuilder))
        .transform(
            new ClosingFunction<TargetAndResult<T>, T>() {
              @Override
              public T apply(DeferredCloser closer, TargetAndResult<T> input) throws Exception {
                closer.eventuallyClose(
                    new Closeable() {
                      @Override
                      public void close() {
                        requestManager.clear(input.target);
                      }
                    },
                    executor);
                return input.result;
              }
            },
            executor);
  }

  /**
   * Convert a pending load request into a ListenableFuture.
   *
   * <p>Sample code:
   *
   * <pre>{@code
   * ListenableFuture<File> image =
   *     GlideFutures.submit(requestManager.asFile().load(url));
   * }</pre>
   *
   * @param requestBuilder A request builder for the resource to load. It must be tied to an
   *     application Glide instance, and must not have a listener set.
   */
  public static <T> ListenableFuture<T> submit(final RequestBuilder<T> requestBuilder) {
    return transformFromTargetAndResult(submitInternal(requestBuilder));
  }

  private static <T> ListenableFuture<T> transformFromTargetAndResult(
      ListenableFuture<TargetAndResult<T>> future) {
    return Futures.transform(
        future,
        new Function<TargetAndResult<T>, T>() {
          @Override
          public T apply(TargetAndResult<T> input) {
            return input.result;
          }
        },
        Executors.directExecutor());
  }

  private static <T> ListenableFuture<TargetAndResult<T>> submitInternal(
      final RequestBuilder<T> requestBuilder) {
    return CallbackToFutureAdapter.getFuture(
        new Resolver<TargetAndResult<T>>() {
          @Override
          public Object attachCompleter(@NonNull Completer<TargetAndResult<T>> completer) {
            GlideLoadingListener<T> listener = new GlideLoadingListener<>(completer);
            final FutureTarget<T> futureTarget = requestBuilder.listener(listener).submit();
            completer.addCancellationListener(
                new Runnable() {
                  @Override
                  public void run() {
                    futureTarget.cancel(/*mayInterruptIfRunning=*/ true);
                  }
                },
                MoreExecutors.directExecutor());
            return listener;
          }
        });
  }

  /** Listener to convert Glide load results into ListenableFutures. */
  private static final class GlideLoadingListener<T> implements RequestListener<T> {

    private final Completer<TargetAndResult<T>> completer;

    GlideLoadingListener(Completer<TargetAndResult<T>> completer) {
      this.completer = completer;
    }

    @Override
    public boolean onLoadFailed(
        @Nullable GlideException e, Object model, Target<T> target, boolean isFirst) {
      completer.setException(e != null ? e : new RuntimeException("Unknown error"));
      return true;
    }

    @Override
    public boolean onResourceReady(
        T resource, Object model, Target<T> target, DataSource dataSource, boolean isFirst) {
      try {
        completer.set(new TargetAndResult<>(target, resource));
      } catch (Throwable t) {
        completer.setException(t);
      }
      return true;
    }
  }

  private static final class TargetAndResult<T> {
    private final Target<T> target;
    private final T result;

    TargetAndResult(Target<T> target, T result) {
      this.target = target;
      this.result = result;
    }
  }

  private GlideFutures() {}
}
