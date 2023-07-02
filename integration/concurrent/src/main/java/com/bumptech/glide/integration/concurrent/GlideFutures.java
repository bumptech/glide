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
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Executor;

/** Utilities for getting ListenableFutures out of Glide. */
public final class GlideFutures {

  /**
   * Preloads the resource for {@code builder} and returns a {@link ListenableFuture} that can be
   * used to monitor status.
   *
   * <p>Shorthand for simply calling {@link #submitAndExecute(RequestManager, RequestBuilder,
   * ResourceConsumer, Executor)} with an empty {@code action}.
   */
  // Wildcard resource types can't be directly instantiated, we don't need to care about the type
  // here.
  @SuppressWarnings({"rawtypes", "unchecked"})
  public static ListenableFuture<Void> preload(
      final RequestManager requestManager, RequestBuilder<?> builder, Executor executor) {
    return submitAndExecute(
        requestManager,
        builder,
        new ResourceConsumer() {
          @Override
          public void act(Object resource) {}
        },
        executor);
  }

  /**
   * Acts on a resource loaded by Glide.
   *
   * @param <T> The type of resource (Bitmap, Drawable etc).
   */
  public interface ResourceConsumer<T> {
    void act(T resource);
  }

  /**
   * Submits the provided request, performs the provided {@code action} and returns a {@link
   * ListenableFuture} that can be used to cancel the request or monitor its status.
   *
   * <p>Cancellation is best effort and may result in some resources not being returned back to
   * Glide's pool. In particular, if the request is cancelled after the resource is loaded by Glide,
   * but before {@code action} is run on {@code executor}, the resource will not be returned. We
   * have the unfortunate choice between unsafely returning resources to the pool immediately when
   * cancel is called while they may still be in use via {@link
   * com.google.common.util.concurrent.ClosingFuture} or occasionally failing to return resources to
   * the pool. Because failing to return resources to the pool is inefficient, but safe, that's the
   * route we've chosen. A more sophisticated implementation may allow us to avoid the resource
   * inefficiency.
   *
   * <p>If you do not need to interact with resource, use {@link #preload(RequestManager,
   * RequestBuilder, Executor)}. {@code preload} is more efficient because it knows that the
   * resource is never used and can always clear the resource immediately on cancellation, unlike
   * this method.
   *
   * <p>An example usage:
   *
   * <pre>{@code
   *   ListenableFuture<Void> future =
   *     submitAndExecute(
   *       requestManager,
   *       requestBuilder,
   *       (bitmap) -> doSomethingWithBitmap(bitmap),
   *       backgroundExecutor);
   * ;
   * }</pre>
   *
   * @param <T> The type of resource that will be loaded (Bitmap, Drawable, etc).
   */
  public static <T> ListenableFuture<Void> submitAndExecute(
      final RequestManager requestManager,
      RequestBuilder<T> requestBuilder,
      final ResourceConsumer<T> action,
      Executor executor) {
    // If the request completes normally, then the target is cleared and the resource is returned.
    // If the request fails while loading the image, there's no need to clear.
    // If the request fails while calling the action, the target is cleared and the resource is
    // returned.
    // If the request is cancelled before the resource is loaded, then the resource is returned
    // If the request is cancelled after the resource is loaded but before the transform runs,
    // then the resource is dropped (but not leaked)
    // If the request is cancelled after the transform method starts, then the resource is returned.
    return FluentFuture.from(submitInternal(requestBuilder))
        .transform(
            new Function<TargetAndResult<T>, Void>() {
              @Override
              public Void apply(TargetAndResult<T> targetAndResult) {
                try {
                  action.act(targetAndResult.result);
                } finally {
                  requestManager.clear(targetAndResult.target);
                }
                return null;
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
   * @param <T> The type of resource that will be loaded (Bitmap, Drawable, etc).
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
          // Only used for toString
          @SuppressWarnings("FutureReturnValueIgnored")
          @Override
          public Object attachCompleter(@NonNull Completer<TargetAndResult<T>> completer) {
            GlideLoadingListener<T> listener = new GlideLoadingListener<>(completer);
            final FutureTarget<T> futureTarget = requestBuilder.addListener(listener).submit();
            completer.addCancellationListener(
                new Runnable() {
                  @Override
                  public void run() {
                    futureTarget.cancel(/* mayInterruptIfRunning= */ true);
                  }
                },
                MoreExecutors.directExecutor());
            return futureTarget;
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
        @Nullable GlideException e, Object model, @NonNull Target<T> target, boolean isFirst) {
      completer.setException(e != null ? e : new RuntimeException("Unknown error"));
      return true;
    }

    @Override
    public boolean onResourceReady(
        @NonNull T resource, @NonNull Object model, Target<T> target, @NonNull DataSource dataSource, boolean isFirst) {
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
