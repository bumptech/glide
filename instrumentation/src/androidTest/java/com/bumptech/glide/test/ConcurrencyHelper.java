package com.bumptech.glide.test;


import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.widget.ImageView;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.target.DrawableImageViewTarget;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Helper for running sections of code on the main thread in emulator tests.
 */
public class ConcurrencyHelper {
  private final Handler handler = new Handler(Looper.getMainLooper());
  private static final long TIMEOUT_MS = 5000;
  private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;

  public <T> T get(Future<T> future) {
    try {
      return future.get(TIMEOUT_MS, TIMEOUT_UNIT);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  public <T, Y extends Future<T>> Y wait(Y future) {
    get(future);
    return future;
  }

  public Target<Drawable> loadOnMainThread(
      final RequestBuilder<Drawable> builder, ImageView imageView) {
    return loadOnMainThread(builder, new DrawableImageViewTarget(imageView));
  }

  public <T> Target<T> loadOnMainThread(final RequestBuilder<T> builder, final Target<T> target) {
    return callOnMainThread(new Callable<Target<T>>() {
      @Override
      public Target<T> call() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        builder.into(new Target<T>() {
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
          public void onResourceReady(T resource, Transition<? super T> transition) {
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
          public void getSize(SizeReadyCallback cb) {
            target.getSize(cb);
          }

          @Override
          public void removeCallback(SizeReadyCallback cb) {
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
        latch.await(TIMEOUT_MS, TIMEOUT_UNIT);
        return target;
      }
    });
  }

  public void pokeMainThread() {
    runOnMainThread(new Runnable() {
      @Override
      public void run() {
        // Do nothing.
      }
    });
  }

  public void runOnMainThread(final Runnable runnable) {
    callOnMainThread(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        runnable.run();
        return null;
      }
    });
  }

  public <T> T callOnMainThread(final Callable<T> callable) {
    final AtomicReference<T> reference = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    handler.post(new Runnable() {
      @Override
      public void run() {
        try {
          T result = callable.call();
          reference.set(result);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
        latch.countDown();
      }
    });
    try {
      latch.await(TIMEOUT_MS, TIMEOUT_UNIT);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return reference.get();
  }
}
