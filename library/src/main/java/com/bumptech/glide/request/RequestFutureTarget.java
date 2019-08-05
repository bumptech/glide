package com.bumptech.glide.request;

import android.graphics.drawable.Drawable;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.util.Util;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A {@link java.util.concurrent.Future} implementation for Glide that can be used to load resources
 * in a blocking manner on background threads.
 *
 * <p>Note - Unlike most targets, RequestFutureTargets can be used once and only once. Attempting to
 * reuse a RequestFutureTarget will probably result in undesirable behavior or exceptions. Instead
 * of reusing objects of this class, the pattern should be:
 *
 * <pre>{@code
 * FutureTarget<File> target = null;
 * RequestManager requestManager = Glide.with(context);
 * try {
 *   target = requestManager
 *      .downloadOnly()
 *      .load(model)
 *      .submit();
 *   File downloadedFile = target.get();
 *   // ... do something with the file (usually throws IOException)
 * } catch (ExecutionException | InterruptedException | IOException e) {
 *   // ... bug reporting or recovery
 * } finally {
 *   // make sure to cancel pending operations and free resources
 *   if (target != null) {
 *     target.cancel(true); // mayInterruptIfRunning
 *   }
 * }
 * }</pre>
 *
 * The {@link #cancel(boolean)} call will cancel pending operations and make sure that any resources
 * used are recycled.
 *
 * @param <R> The type of the resource that will be loaded.
 */
public class RequestFutureTarget<R> implements FutureTarget<R>, RequestListener<R> {
  private static final Waiter DEFAULT_WAITER = new Waiter();

  private final int width;
  private final int height;
  // Exists for testing only.
  private final boolean assertBackgroundThread;
  private final Waiter waiter;

  @GuardedBy("this")
  @Nullable
  private R resource;

  @GuardedBy("this")
  @Nullable
  private Request request;

  @GuardedBy("this")
  private boolean isCancelled;

  @GuardedBy("this")
  private boolean resultReceived;

  @GuardedBy("this")
  private boolean loadFailed;

  @GuardedBy("this")
  @Nullable
  private GlideException exception;

  /** Constructor for a RequestFutureTarget. Should not be used directly. */
  public RequestFutureTarget(int width, int height) {
    this(width, height, true, DEFAULT_WAITER);
  }

  RequestFutureTarget(int width, int height, boolean assertBackgroundThread, Waiter waiter) {
    this.width = width;
    this.height = height;
    this.assertBackgroundThread = assertBackgroundThread;
    this.waiter = waiter;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    Request toClear = null;
    synchronized (this) {
      if (isDone()) {
        return false;
      }

      isCancelled = true;
      waiter.notifyAll(this);
      if (mayInterruptIfRunning) {
        toClear = request;
        request = null;
      }
    }

    // Avoid deadlock by clearing outside of the lock (b/138335419)
    if (toClear != null) {
      toClear.clear();
    }
    return true;
  }

  @Override
  public synchronized boolean isCancelled() {
    return isCancelled;
  }

  @Override
  public synchronized boolean isDone() {
    return isCancelled || resultReceived || loadFailed;
  }

  @Override
  public R get() throws InterruptedException, ExecutionException {
    try {
      return doGet(null);
    } catch (TimeoutException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public R get(long time, @NonNull TimeUnit timeUnit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return doGet(timeUnit.toMillis(time));
  }

  /** A callback that should never be invoked directly. */
  @Override
  public void getSize(@NonNull SizeReadyCallback cb) {
    cb.onSizeReady(width, height);
  }

  @Override
  public void removeCallback(@NonNull SizeReadyCallback cb) {
    // Do nothing because we do not retain references to SizeReadyCallbacks.
  }

  @Override
  public synchronized void setRequest(@Nullable Request request) {
    this.request = request;
  }

  @Override
  @Nullable
  public synchronized Request getRequest() {
    return request;
  }

  /** A callback that should never be invoked directly. */
  @Override
  public void onLoadCleared(@Nullable Drawable placeholder) {
    // Do nothing.
  }

  /** A callback that should never be invoked directly. */
  @Override
  public void onLoadStarted(@Nullable Drawable placeholder) {
    // Do nothing.
  }

  /** A callback that should never be invoked directly. */
  @Override
  public synchronized void onLoadFailed(@Nullable Drawable errorDrawable) {
    // Ignored, synchronized for backwards compatibility.
  }

  /** A callback that should never be invoked directly. */
  @Override
  public synchronized void onResourceReady(
      @NonNull R resource, @Nullable Transition<? super R> transition) {
    // Ignored, synchronized for backwards compatibility.
  }

  private synchronized R doGet(Long timeoutMillis)
      throws ExecutionException, InterruptedException, TimeoutException {
    if (assertBackgroundThread && !isDone()) {
      Util.assertBackgroundThread();
    }

    if (isCancelled) {
      throw new CancellationException();
    } else if (loadFailed) {
      throw new ExecutionException(exception);
    } else if (resultReceived) {
      return resource;
    }

    if (timeoutMillis == null) {
      waiter.waitForTimeout(this, 0);
    } else if (timeoutMillis > 0) {
      long now = System.currentTimeMillis();
      long deadline = now + timeoutMillis;
      while (!isDone() && now < deadline) {
        waiter.waitForTimeout(this, deadline - now);
        now = System.currentTimeMillis();
      }
    }

    if (Thread.interrupted()) {
      throw new InterruptedException();
    } else if (loadFailed) {
      throw new ExecutionException(exception);
    } else if (isCancelled) {
      throw new CancellationException();
    } else if (!resultReceived) {
      throw new TimeoutException();
    }

    return resource;
  }

  @Override
  public void onStart() {
    // Do nothing.
  }

  @Override
  public void onStop() {
    // Do nothing.
  }

  @Override
  public void onDestroy() {
    // Do nothing.
  }

  @Override
  public synchronized boolean onLoadFailed(
      @Nullable GlideException e, Object model, Target<R> target, boolean isFirstResource) {
    loadFailed = true;
    exception = e;
    waiter.notifyAll(this);
    return false;
  }

  @Override
  public synchronized boolean onResourceReady(
      R resource, Object model, Target<R> target, DataSource dataSource, boolean isFirstResource) {
    // We might get a null result.
    resultReceived = true;
    this.resource = resource;
    waiter.notifyAll(this);
    return false;
  }

  @VisibleForTesting
  static class Waiter {
    // This is a simple wrapper class that is used to enable testing. The call to the wrapping class
    // is waited on appropriately.
    @SuppressWarnings("WaitNotInLoop")
    void waitForTimeout(Object toWaitOn, long timeoutMillis) throws InterruptedException {
      toWaitOn.wait(timeoutMillis);
    }

    void notifyAll(Object toNotify) {
      toNotify.notifyAll();
    }
  }
}
