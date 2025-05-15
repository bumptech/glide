package com.bumptech.glide.benchmark;

import android.app.Application;
import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.benchmark.BenchmarkState;
import androidx.benchmark.junit4.BenchmarkRule;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.testutil.ConcurrencyHelper;
import com.bumptech.glide.testutil.TearDownGlide;
import com.google.common.base.Preconditions;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Simulate loading a file from Glide's cache as a thumbnail in various sizes. */
@RunWith(AndroidJUnit4.class)
public class BenchmarkFromCache {
  private final ConcurrencyHelper concurrencyHelper = new ConcurrencyHelper();
  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();
  @Rule public final BenchmarkRule benchmarkRule = new BenchmarkRule();

  private final Application app = ApplicationProvider.getApplicationContext();

  @Test
  public void pixel3a_portrait_original() throws Exception {
    runBenchmark(R.raw.pixel3a_portrait, Target.SIZE_ORIGINAL);
  }

  @Test
  public void pixel3a_portrait_large() throws Exception {
    runBenchmark(R.raw.pixel3a_portrait, 2048);
  }

  @Test
  public void pixel3a_portrait_medium() throws Exception {
    runBenchmark(R.raw.pixel3a_portrait, 1024);
  }

  @Test
  public void pixel3a_portrait_small() throws Exception {
    runBenchmark(R.raw.pixel3a_portrait, 256);
  }

  @Test
  public void pixel3a_portrait_tiny() throws Exception {
    runBenchmark(R.raw.pixel3a_portrait, 50);
  }

  private void runBenchmark(@RawRes final int resourceId, final int targetSize) throws Exception {
    BenchmarkState state = benchmarkRule.getState();
    state.pauseTiming();
    // Writes to the disk cache happen asynchronously after a request completes. To make sure we're
    // only timing reads from disk cache, we need to make sure we wait until that async write
    // finishes. This is a simple, albiet hacky, way to accomplish that.
    try {
      while (true) {
        loadImageWithExpectedDataSource(
            state, resourceId, targetSize, DataSource.LOCAL, /* isAlreadyPaused= */ true);
      }
    } catch (IllegalStateException e) {
      // Now that we're no longer getting LOCAL as our data source, it's safe to proceed.
    }
    state.resumeTiming();

    while (state.keepRunning()) {
      state.pauseTiming();
      clearMemoryCache();
      state.resumeTiming();

      loadImageWithExpectedDataSource(
          state,
          resourceId,
          targetSize,
          DataSource.RESOURCE_DISK_CACHE,
          /* isAlreadyPaused= */ false);
    }
  }

  private void loadImageWithExpectedDataSource(
      BenchmarkState state,
      @RawRes int resourceId,
      int targetSize,
      DataSource expectedDataSource,
      boolean isAlreadyPaused)
      throws InterruptedException, ExecutionException, TimeoutException {
    final AtomicReference<DataSource> dataSourceRef = new AtomicReference<>();
    FutureTarget<Bitmap> target =
        Glide.with(app)
            .asBitmap()
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .skipMemoryCache(true)
            .override(targetSize)
            .load(resourceId)
            .listener(
                new RequestListener<Bitmap>() {
                  @Override
                  public boolean onLoadFailed(
                      @Nullable GlideException e,
                      Object model,
                      @NonNull Target<Bitmap> target,
                      boolean isFirstResource) {
                    return false;
                  }

                  @Override
                  public boolean onResourceReady(
                      @NonNull Bitmap resource,
                      @NonNull Object model,
                      Target<Bitmap> target,
                      @NonNull DataSource dataSource,
                      boolean isFirstResource) {
                    dataSourceRef.set(dataSource);
                    return false;
                  }
                })
            .submit();
    target.get(15, TimeUnit.SECONDS);

    if (!isAlreadyPaused) {
      state.pauseTiming();
    }

    Preconditions.checkState(dataSourceRef.get() == expectedDataSource, dataSourceRef.get());
    Glide.with(app).clear(target);

    if (!isAlreadyPaused) {
      state.resumeTiming();
    }
  }

  private void clearMemoryCache() {
    concurrencyHelper.runOnMainThread(
        new Runnable() {
          @Override
          public void run() {
            Glide.get(app).clearMemory();
          }
        });
  }
}
