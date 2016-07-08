package com.bumptech.glide.request;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.annotation.Nullable;
import com.bumptech.glide.request.target.SizeReadyCallback;
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
 * <p> Note - Unlike most targets, RequestFutureTargets can be used once and only once. Attempting
 * to reuse a RequestFutureTarget will probably result in undesirable behavior or exceptions.
 * Instead of reusing objects of this class, the pattern should be:
 *
 * <pre>
 *     {@code
 *      FutureTarget<File> target = null;
 *      RequestManager requestManager = Glide.with(context);
 *      try {
 *        target = requestManager
 *           .downloadOnly()
 *           .load(model)
 *           .submit();
 *        File downloadedFile = target.get();
 *        // ... do something with the file (usually throws IOException)
 *      } catch (ExecutionException | InterruptedException | IOException e) {
 *        // ... bug reporting or recovery
 *      } finally {
 *        // make sure to cancel pending operations and free resources
 *        if (target != null) {
 *          target.cancel(true); // mayInterruptIfRunning
 *        }
 *      }
 *     }
 *     </pre>
 * The {@link #cancel(boolean)} call will cancel pending operations and
 * make sure that any resources used are recycled.
 * </p>
 *
 * @param <R> The type of the resource that will be loaded.
 */
public class RequestFutureTarget<R> implements FutureTarget<R>,
    Runnable {
  private static final Waiter DEFAULT_WAITER = new Waiter();

  private final Handler mainHandler;
  private final int width;
  private final int height;
  // Exists for testing only.
  private final boolean assertBackgroundThread;
  private final Waiter waiter;

  @Nullable private R resource;
  @Nullable private Request request;
  private boolean isCancelled;
  private boolean resultReceived;
  private boolean loadFailed;

  /**
   * Constructor for a RequestFutureTarget. Should not be used directly.
   */
  public RequestFutureTarget(Handler mainHandler, int width, int height) {
    this(mainHandler, width, height, true, DEFAULT_WAITER);
  }

  RequestFutureTarget(Handler mainHandler, int width, int height, boolean assertBackgroundThread,
      Waiter waiter) {
    this.mainHandler = mainHandler;
    this.width = width;
    this.height = height;
    this.assertBackgroundThread = assertBackgroundThread;
    this.waiter = waiter;
  }

  @Override
  public synchronized boolean cancel(boolean mayInterruptIfRunning) {
    if (isCancelled) {
      return true;
    }

    final boolean result = !isDone();
    if (result) {
      isCancelled = true;
      waiter.notifyAll(this);
    }
    clearOnMainThread();
    return result;
  }

  @Override
  public synchronized boolean isCancelled() {
    return isCancelled;
  }

  @Override
  public synchronized boolean isDone() {
    return isCancelled || resultReceived;
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
  public R get(long time, TimeUnit timeUnit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return doGet(timeUnit.toMillis(time));
  }

  /**
   * A callback that should never be invoked directly.
   */
  @Override
  public void getSize(SizeReadyCallback cb) {
    cb.onSizeReady(width, height);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setRequest(@Nullable Request request) {
    this.request = request;
  }

  @Override
  @Nullable
  public Request getRequest() {
    return request;
  }

  /**
   * A callback that should never be invoked directly.
   */
  @Override
  public void onLoadCleared(Drawable placeholder) {
    // Do nothing.
  }

  /**
   * A callback that should never be invoked directly.
   */
  @Override
  public void onLoadStarted(Drawable placeholder) {
    // Do nothing.
  }

  /**
   * A callback that should never be invoked directly.
   */
  @Override
  public synchronized void onLoadFailed(Drawable errorDrawable) {
    loadFailed = true;
    waiter.notifyAll(this);
  }

  /**
   * A callback that should never be invoked directly.
   */
  @Override
  public synchronized void onResourceReady(R resource, Transition<? super R> transition) {
    // We might get a null result.
    resultReceived = true;
    this.resource = resource;
    waiter.notifyAll(this);
  }

  private synchronized R doGet(Long timeoutMillis)
      throws ExecutionException, InterruptedException, TimeoutException {
    if (assertBackgroundThread && !isDone()) {
      Util.assertBackgroundThread();
    }

    if (isCancelled) {
      throw new CancellationException();
    } else if (loadFailed) {
      throw new ExecutionException(new IllegalStateException("Load failed"));
    } else if (resultReceived) {
      return resource;
    }

    if (timeoutMillis == null) {
      waiter.waitForTimeout(this, 0);
    } else if (timeoutMillis > 0) {
      waiter.waitForTimeout(this, timeoutMillis);
    }

    if (Thread.interrupted()) {
      throw new InterruptedException();
    } else if (loadFailed) {
      throw new ExecutionException(new IllegalStateException("Load failed"));
    } else if (isCancelled) {
      throw new CancellationException();
    } else if (!resultReceived) {
      throw new TimeoutException();
    }

    return resource;
  }

  /**
   * A callback that should never be invoked directly.
   */
  @Override
  public void run() {
    if (request != null) {
      request.clear();
      request = null;
    }
  }

  private void clearOnMainThread() {
    mainHandler.post(this);
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

  // Visible for testing.
  static class Waiter {

    public void waitForTimeout(Object toWaitOn, long timeoutMillis) throws InterruptedException {
      toWaitOn.wait(timeoutMillis);
    }

    public void notifyAll(Object toNotify) {
      toNotify.notifyAll();
    }
  }
}
