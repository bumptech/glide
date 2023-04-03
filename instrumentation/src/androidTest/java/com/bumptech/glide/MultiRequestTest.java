package com.bumptech.glide;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.engine.executor.GlideExecutor;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.test.ModelGeneratorRule;
import com.bumptech.glide.testutil.ConcurrencyHelper;
import com.bumptech.glide.testutil.TearDownGlide;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MultiRequestTest {
  private final Context context = ApplicationProvider.getApplicationContext();
  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();
  @Rule public final ModelGeneratorRule modelGeneratorRule = new ModelGeneratorRule();
  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();
  private final ConcurrencyHelper concurrency = new ConcurrencyHelper();

  @Test
  public void thumbnail_onResourceReady_forPrimary_isComplete_whenRequestListenerIsCalled()
      throws IOException, InterruptedException {

    // Make sure the requests complete in the same order
    Glide.init(
        context,
        new GlideBuilder()
            .setSourceExecutor(GlideExecutor.newSourceBuilder().setThreadCount(1).build()));

    AtomicBoolean isPrimaryRequestComplete = new AtomicBoolean(false);
    CountDownLatch countDownLatch = new CountDownLatch(1);

    RequestBuilder<Drawable> request =
        Glide.with(context)
            .load(newImageFile())
            .thumbnail(Glide.with(context).load(newImageFile()))
            .listener(
                new RequestListener<>() {
                  @Override
                  public boolean onLoadFailed(
                      @Nullable GlideException e,
                      Object model,
                      Target<Drawable> target,
                      boolean isFirstResource) {
                    return false;
                  }

                  @Override
                  public boolean onResourceReady(
                      Drawable resource,
                      Object model,
                      Target<Drawable> target,
                      DataSource dataSource,
                      boolean isFirstResource) {
                    isPrimaryRequestComplete.set(target.getRequest().isComplete());
                    countDownLatch.countDown();
                    return false;
                  }
                });
    concurrency.runOnMainThread(() -> request.into(new DoNothingTarget()));

    assertThat(countDownLatch.await(3, TimeUnit.SECONDS)).isTrue();
    assertThat(isPrimaryRequestComplete.get()).isTrue();
  }

  @Test
  public void thumbnail_onLoadFailed_forPrimary_isNotRunningOrComplete_whenRequestListenerIsCalled()
      throws IOException, InterruptedException {

    // Make sure the requests complete in the same order
    Glide.init(
        context,
        new GlideBuilder()
            .setSourceExecutor(GlideExecutor.newSourceBuilder().setThreadCount(1).build()));

    AtomicBoolean isNeitherRunningNorComplete = new AtomicBoolean(false);
    CountDownLatch countDownLatch = new CountDownLatch(1);

    int missingResourceId = 123;
    RequestBuilder<Drawable> requestBuilder =
        Glide.with(context)
            .load(missingResourceId)
            .thumbnail(Glide.with(context).load(newImageFile()))
            .listener(
                new RequestListener<>() {
                  @Override
                  public boolean onLoadFailed(
                      @Nullable GlideException e,
                      Object model,
                      Target<Drawable> target,
                      boolean isFirstResource) {
                    Request request = target.getRequest();
                    isNeitherRunningNorComplete.set(!request.isComplete() && !request.isRunning());
                    countDownLatch.countDown();
                    return false;
                  }

                  @Override
                  public boolean onResourceReady(
                      Drawable resource,
                      Object model,
                      Target<Drawable> target,
                      DataSource dataSource,
                      boolean isFirstResource) {
                    return false;
                  }
                });
    concurrency.runOnMainThread(() -> requestBuilder.into(new DoNothingTarget()));

    assertThat(countDownLatch.await(3, TimeUnit.SECONDS)).isTrue();
    assertThat(isNeitherRunningNorComplete.get()).isTrue();
  }

  private File newImageFile() throws IOException {
    Bitmap bitmap = Bitmap.createBitmap(100, 100, Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    canvas.drawColor(Color.RED);
    File result = temporaryFolder.newFile();
    try (OutputStream os = new BufferedOutputStream(new FileOutputStream(result))) {
      bitmap.compress(CompressFormat.JPEG, 75, os);
    }
    return result;
  }

  // We don't store or do anything with the resource, so we don't need to do anything to release it
  // in onLoadCleared.
  private static final class DoNothingTarget extends CustomTarget<Drawable> {
    @Override
    public void onResourceReady(
        @NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {}

    @Override
    public void onLoadCleared(@Nullable Drawable placeholder) {}
  }
}
