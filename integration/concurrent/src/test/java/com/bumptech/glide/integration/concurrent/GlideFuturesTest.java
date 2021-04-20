package com.bumptech.glide.integration.concurrent;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.executor.GlideExecutor;
import com.bumptech.glide.load.engine.executor.MockGlideExecutor;
import com.bumptech.glide.testutil.MockModelLoader;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class GlideFuturesTest {

  private Context app;

  @Before
  public void setUp() {
    app = ApplicationProvider.getApplicationContext();

    GlideExecutor executor = MockGlideExecutor.newMainThreadExecutor();
    Glide.init(
        app,
        new GlideBuilder()
            .setAnimationExecutor(executor)
            .setSourceExecutor(executor)
            .setDiskCacheExecutor(executor));
  }

  @Test
  public void testBaseLoad() throws Exception {
    ColorDrawable expected = new ColorDrawable(Color.RED);
    ListenableFuture<Drawable> future = GlideFutures.submit(Glide.with(app).load(expected));
    assertThat(((ColorDrawable) Futures.getDone(future)).getColor()).isEqualTo(expected.getColor());
  }

  @Test
  public void testErrorLoad() {
    // Load some unsupported model.
    final ListenableFuture<Bitmap> future =
        GlideFutures.submit(Glide.with(app).asBitmap().load(app));
    // Make sure that it throws.
    assertThrows(
        ExecutionException.class,
        new ThrowingRunnable() {
          @Override
          public void run() throws Throwable {
            Futures.getDone(future);
          }
        });
  }

  @Test
  public void testToString() throws Exception {
    Foo model = new Foo();
    SettableFuture<Bar> bar = SettableFuture.create();

    Glide.get(app)
        .getRegistry()
        .prepend(
            Bar.class,
            Baz.class,
            new ResourceDecoder<Bar, Baz>() {

              @Override
              public boolean handles(Bar source, Options options) throws IOException {
                return true;
              }

              @Override
              public Resource<Baz> decode(Bar source, int width, int height, Options options)
                  throws IOException {
                throw new IOException();
              }
            });
    MockModelLoader.mockAsync(model, Bar.class, bar);
    ListenableFuture<Baz> future =
        GlideFutures.submit(
            Glide.with(app)
                .as(Baz.class)
                .load(model)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE));
    assertThat(future.toString()).contains("Foo");
    future.cancel(true);
    assertThat(bar.isCancelled()).isTrue();
  }

  private static final class Foo {}

  private static final class Bar {}

  private static final class Baz {}
}
