package com.bumptech.glide.test;

import android.graphics.drawable.Drawable;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.InstrumentationRegistry;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.target.DrawableImageViewTarget;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.util.Executors;
import com.bumptech.glide.util.Preconditions;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Helper for running sections of code on the main thread in emulator tests. */
public class ConcurrencyHelper {
  private final Handler handler = new Handler(Looper.getMainLooper());
  private static final long TIMEOUT_SECONDS = 10;
  private static final TimeUnit TIMEOUT_UNIT = TimeUnit.SECONDS;

  public <T> T get(final Future<T> future) {
    final AtomicReference<T> reference = new AtomicReference<>();
    wait(
        new Waiter() {
          @Override
          public boolean await(long timeout, TimeUnit timeUnit) throws InterruptedException {
            try {
              reference.set(future.get(timeout, timeUnit));
              return true;
            } catch (ExecutionException e) {
              throw new RuntimeException(e.getCause());
            } catch (TimeoutException e) {
              return false;
            }
          }
        });
    return reference.get();
  }

  public <T> Target<T> wait(FutureTarget<T> future) {
    get(future);
    return future;
  }

  public void loadOnOtherThread(final Runnable runnable) {
    final AtomicBoolean isDone = new AtomicBoolean();
    final Thread thread =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                runnable.run();
                isDone.set(true);
              }
            });
    thread.start();

    wait(
        new Waiter() {
          @Override
          public boolean await(long timeout, TimeUnit timeUnit) throws InterruptedException {
            thread.join(timeUnit.toMillis(timeout));
            return isDone.get();
          }
        });
  }

  public void loadOnMainThread(final RequestBuilder<Drawable> builder, ImageView imageView) {
    loadOnMainThread(builder, new DrawableImageViewTarget(imageView));
  }

  public void clearOnMainThread(final ImageView imageView) {
    runOnMainThread(
        new Runnable() {
          @Override
          public void run() {
            Glide.with(InstrumentationRegistry.getTargetContext()).clear(imageView);
          }
        });
  }

  public void loadUntilFirstFinish(final RequestBuilder<Drawable> builder, ImageView imageView) {
    loadUntilFirstFinish(builder, new DrawableImageViewTarget(imageView));
  }

  private <T> void loadUntilFirstFinish(final RequestBuilder<T> builder, final Target<T> target) {
    final CountDownLatch latch = new CountDownLatch(1);
    callOnMainThread(
        new Callable<Target<T>>() {
          @Override
          public Target<T> call() {
            builder.into(
                new Target<T>() {
                  @Override
                  public void onStart() {
                    target.onStart();
                  }

                  @Override
                  public void onStop() {
                    target.onStop();
                  }

                  @Override
                  public void onDestroy() {
                    target.onDestroy();
                  }

                  @Override
                  public void onResourceReady(
                      @NonNull T resource, @Nullable Transition<? super T> transition) {
                    target.onResourceReady(resource, transition);
                    latch.countDown();
                  }

                  @Override
                  public void onLoadCleared(@Nullable Drawable placeholder) {
                    target.onLoadCleared(placeholder);
                  }

                  @Override
                  public void onLoadStarted(@Nullable Drawable placeholder) {
                    target.onLoadStarted(placeholder);
                  }

                  @Override
                  public void onLoadFailed(@Nullable Drawable errorDrawable) {
                    target.onLoadFailed(errorDrawable);
                    latch.countDown();
                  }

                  @Override
                  public void getSize(@NonNull SizeReadyCallback cb) {
                    target.getSize(cb);
                  }

                  @Override
                  public void removeCallback(@NonNull SizeReadyCallback cb) {
                    target.removeCallback(cb);
                  }

                  @Override
                  public void setRequest(@Nullable Request request) {
                    target.setRequest(request);
                  }

                  @Nullable
                  @Override
                  public Request getRequest() {
                    return target.getRequest();
                  }
                });
            return target;
          }
        });
    waitOnLatch(latch);
  }

  private <T> void loadOnMainThread(final RequestBuilder<T> builder, final Target<T> target) {
    final CountDownLatch latch = new CountDownLatch(1);
    callOnMainThread(
        new Callable<Target<T>>() {
          @Override
          public Target<T> call() {
            builder.into(
                new Target<T>() {
                  @Override
                  public void onStart() {
                    target.onStart();
                  }

                  @Override
                  public void onStop() {
                    target.onStop();
                  }

                  @Override
                  public void onDestroy() {
                    target.onDestroy();
                  }

                  @Override
                  public void onResourceReady(
                      @NonNull T resource, @Nullable Transition<? super T> transition) {
                    target.onResourceReady(resource, transition);
                    checkRequestAndMaybeReleaseLatch();
                  }

                  @Override
                  public void onLoadCleared(@Nullable Drawable placeholder) {
                    target.onLoadCleared(placeholder);
                  }

                  @Override
                  public void onLoadStarted(@Nullable Drawable placeholder) {
                    target.onLoadStarted(placeholder);
                  }

                  @Override
                  public void onLoadFailed(@Nullable Drawable errorDrawable) {
                    target.onLoadFailed(errorDrawable);
                    checkRequestAndMaybeReleaseLatch();
                  }

                  @Override
                  public void getSize(@NonNull SizeReadyCallback cb) {
                    target.getSize(cb);
                  }

                  @Override
                  public void removeCallback(@NonNull SizeReadyCallback cb) {
                    target.removeCallback(cb);
                  }

                  @Override
                  public void setRequest(@Nullable Request request) {
                    target.setRequest(request);
                  }

                  @Nullable
                  @Override
                  public Request getRequest() {
                    return target.getRequest();
                  }

                  // We can't guarantee the ordering of when this callback is called and when the
                  // request's state is updated, so it's safer to post the check back to the UI
                  // thread.
                  private void checkRequestAndMaybeReleaseLatch() {
                    Executors.mainThreadExecutor()
                        .execute(
                            new Runnable() {
                              @Override
                              public void run() {
                                if (!Preconditions.checkNotNull(getRequest()).isRunning()) {
                                  latch.countDown();
                                }
                              }
                            });
                  }
                });
            return target;
          }
        });
    waitOnLatch(latch);
  }

  public void pokeMainThread() {
    runOnMainThread(
        new Runnable() {
          @Override
          public void run() {
            // Do nothing.
          }
        });
  }

  public void runOnMainThread(final Runnable runnable) {
    callOnMainThread(
        new Callable<Void>() {
          @Override
          public Void call() {
            runnable.run();
            return null;
          }
        });
  }

  private <T> void callOnMainThread(final Callable<T> callable) {
    final CountDownLatch latch = new CountDownLatch(1);
    handler.post(
        new Runnable() {
          @Override
          public void run() {
            try {
              callable.call();
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
            latch.countDown();
          }
        });
    waitOnLatch(latch);
  }

  public static void waitOnLatch(final CountDownLatch latch) {
    wait(
        new Waiter() {
          @Override
          public boolean await(long timeout, TimeUnit timeUnit) throws InterruptedException {
            return latch.await(timeout, timeUnit);
          }
        });
  }

  private interface Waiter {
    boolean await(long timeout, TimeUnit timeUnit) throws InterruptedException;
  }

  private static void wait(Waiter waiter) {
    boolean isFinished = false;
    do {
      try {
        try {
          isFinished = waiter.await(TIMEOUT_SECONDS, TIMEOUT_UNIT);
          if (!isFinished) {
            throw new WaiterException("Timed out while waiting");
          }
        } catch (InterruptedException e) {
          throw new WaiterException(e);
        }
      } catch (WaiterException e) {
        if (Debug.isDebuggerConnected()) {
          continue;
        }
        throw e;
      }
    } while (Debug.isDebuggerConnected() && !isFinished);
  }

  private static final class WaiterException extends RuntimeException {
    private static final long serialVersionUID = -627297254223169728L;

    WaiterException(String message) {
      super(message);
    }

    WaiterException(Throwable cause) {
      super(cause);
    }
  }
}
