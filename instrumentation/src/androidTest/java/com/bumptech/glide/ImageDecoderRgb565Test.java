package com.bumptech.glide;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.os.Build;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.load.resource.bitmap.Downsampler;
import com.bumptech.glide.test.FakeStreamModelLoader;
import com.bumptech.glide.test.ResourceIds;
import com.bumptech.glide.testutil.ConcurrencyHelper;
import com.bumptech.glide.testutil.TearDownGlide;
import com.bumptech.glide.util.Util;
import java.io.InputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests decoding bitmaps using {@link DecodeFormat#PREFER_RGB_565} when {@link ImageDecoder} is
 * enabled (API 29+), verifying that RGB_565 allocations use 2 bytes per pixel rather than 4 bytes
 * per pixel, both when {@link ImageDecoder} handles decoding directly and when falling back to
 * {@link Downsampler}.
 */
@RunWith(AndroidJUnit4.class)
public class ImageDecoderRgb565Test {
  private static final int BITMAP_POOL_SIZE_BYTES = 10 * 1024 * 1024;
  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();
  private final ConcurrencyHelper concurrency = new ConcurrencyHelper();
  private final Context context = ApplicationProvider.getApplicationContext();

  @Test
  public void loadBmpResource_withPreferRgb565AndForceFallback_allocatesRgb565() {
    assumeTrue(
        "ImageDecoder was added in O but Glide enables it Q+",
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q);

    BitmapPool pool = new LruBitmapPool(BITMAP_POOL_SIZE_BYTES);
    Bitmap expectedBitmap = Bitmap.createBitmap(320, 240, Bitmap.Config.RGB_565);
    pool.put(expectedBitmap);
    Glide.init(
        context,
        new GlideBuilder()
            .setBitmapPool(pool)
            .setEnableRgb565DownsamplerFix(true)
            .setImageDecoderEnabledForBitmaps(true)
            .setUseArrayPoolForImageDecoderByteBufferAllocation(true));

    Glide.get(context)
        .getRegistry()
        .prepend(
            Object.class,
            InputStream.class,
            new FakeStreamModelLoader<>(context, ResourceIds.raw.canonical_bmp));

    Bitmap bitmap =
        concurrency.get(
            Glide.with(context)
                .asBitmap()
                .format(DecodeFormat.PREFER_RGB_565)
                .load(new Object())
                .override(320, 240)
                .submit());

    assertThat(bitmap).isNotNull();
    assertThat(bitmap.getConfig()).isEqualTo(Bitmap.Config.RGB_565);

    // Verify fallback to Downsampler (which reuses the expectedBitmap from BitmapPool)
    assertThat(bitmap).isSameInstanceAs(expectedBitmap);

    // Verify 2 bytes allocated per pixel (RGB_565) not 4 (ARGB_8888)
    int expectedByteCount = bitmap.getWidth() * bitmap.getHeight() * 2;
    assertThat(expectedByteCount).isGreaterThan(0);
    assertThat(Util.getBitmapByteSize(bitmap)).isEqualTo(expectedByteCount);
  }

  /* Verify no allocation size bug with ImageDecoder enabled and RGB_565 */
  @Test
  public void loadJpegResource_noFallback_withPreferRgb565_allocatesRgb565() {
    assumeTrue(
        "ImageDecoder was added in O but Glide enables it Q+",
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q);

    BitmapPool pool = new LruBitmapPool(BITMAP_POOL_SIZE_BYTES);
    Bitmap expectedBitmap = Bitmap.createBitmap(320, 240, Bitmap.Config.RGB_565);
    pool.put(expectedBitmap);
    Glide.init(
        context,
        new GlideBuilder()
            .setBitmapPool(pool)
            .setEnableRgb565DownsamplerFix(false)
            .setImageDecoderEnabledForBitmaps(true)
            .setUseArrayPoolForImageDecoderByteBufferAllocation(true));

    Glide.get(context)
        .getRegistry()
        .prepend(
            Object.class,
            InputStream.class,
            new FakeStreamModelLoader<>(context, ResourceIds.raw.canonical));

    Bitmap bitmap =
        concurrency.get(
            Glide.with(context)
                .asBitmap()
                .format(DecodeFormat.PREFER_RGB_565)
                .load(new Object())
                .override(320, 240)
                .submit());

    assertThat(bitmap).isNotNull();
    assertThat(bitmap.getConfig()).isEqualTo(Bitmap.Config.RGB_565);

    // Verify no fallback to Downsampler (ImageDecoder used)
    // Does not pull from the BitmapPool
    assertThat(bitmap).isNotSameInstanceAs(expectedBitmap);

    // Verify 2 bytes allocated per pixel (RGB_565) natively handled by ImageDecoder
    int expectedByteCount = bitmap.getWidth() * bitmap.getHeight() * 2;
    assertThat(expectedByteCount).isGreaterThan(0);
    assertThat(Util.getBitmapByteSize(bitmap)).isEqualTo(expectedByteCount);
  }
}
