package com.bumptech.glide.integration.concurrent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.concurrent.futures.CallbackToFutureAdapter.Completer;
import androidx.concurrent.futures.CallbackToFutureAdapter.Resolver;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

/** Utilities for getting ListenableFutures out of Glide. */
public final class GlideFutures {

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
    return CallbackToFutureAdapter.getFuture(
        new Resolver<T>() {
          @Override
          public Object attachCompleter(@NonNull Completer<T> completer) {
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

    private final Completer<T> completer;

    GlideLoadingListener(Completer<T> completer) {
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
        completer.set(resource);
      } catch (Throwable t) {
        completer.setException(t);
      }
      return true;
    }
  }

  private GlideFutures() {}
}
