package com.bumptech.glide.test;


import android.os.Handler;
import android.os.Looper;
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
