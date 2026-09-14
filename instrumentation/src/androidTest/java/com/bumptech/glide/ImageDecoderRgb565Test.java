package com.bumptech.glide;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.test.FakeStreamModelLoader;
import com.bumptech.glide.test.ResourceIds;
import com.bumptech.glide.testutil.ConcurrencyHelper;
import com.bumptech.glide.testutil.TearDownGlide;
import com.bumptech.glide.util.Util;
import java.io.InputStream;
import org.jspecify.annotations.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests decoding bitmaps using {@link DecodeFormat#PREFER_RGB_565} when {@link ImageDecoder} is
 * enabled (API 29+), verifying that RGB_565 allocations use 2 bytes per pixel rather than 4 bytes
 * per pixel.
 */
@RunWith(AndroidJUnit4.class)
public class ImageDecoderRgb565Test {
  private static final int BITMAP_POOL_SIZE_BYTES = 10 * 1024 * 1024;
  private static final int DEFAULT_WIDTH = 320;
  private static final int DEFAULT_HEIGHT = 240;
  private static final int EXPECTED_RGB_565_BYTES = DEFAULT_WIDTH * DEFAULT_HEIGHT * 2;

  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();
  private final ConcurrencyHelper concurrency = new ConcurrencyHelper();
  private final Context context = ApplicationProvider.getApplicationContext();

  @Before
  public void setUp() {
    assumeTrue("ImageDecoder only enabled on Q+", Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q);
  }

  @Test
  public void loadBmpResource_withPreferRgb565AndForceFallback_returnsRgb565Config() {
    Bitmap expectedBitmap =
        Bitmap.createBitmap(DEFAULT_WIDTH, DEFAULT_HEIGHT, Bitmap.Config.RGB_565);
    initGlideForImageDecoder(
        ResourceIds.raw.canonical_bmp, /* enableRgb565Fix= */ true, expectedBitmap);

    Bitmap bitmap = loadPreferRgb565Bitmap(DEFAULT_WIDTH, DEFAULT_HEIGHT);

    // Verify fallback to Downsampler (which reuses the expectedBitmap from BitmapPool)
    assertThat(bitmap).isSameInstanceAs(expectedBitmap);
    assertThat(bitmap.getConfig()).isEqualTo(Bitmap.Config.RGB_565);
  }

  @Test
  public void loadBmpResource_withPreferRgb565AndForceFallback_sizedForRgb565() {
    Bitmap expectedBitmap =
        Bitmap.createBitmap(DEFAULT_WIDTH, DEFAULT_HEIGHT, Bitmap.Config.RGB_565);
    initGlideForImageDecoder(
        ResourceIds.raw.canonical_bmp, /* enableRgb565Fix= */ true, expectedBitmap);

    Bitmap bitmap = loadPreferRgb565Bitmap(DEFAULT_WIDTH, DEFAULT_HEIGHT);

    // Verify fallback to Downsampler (which reuses the expectedBitmap from BitmapPool)
    assertThat(bitmap).isSameInstanceAs(expectedBitmap);
    // Verify 2 bytes allocated per pixel (RGB_565) not 4 (ARGB_8888)
    assertThat(Util.getBitmapByteSize(bitmap)).isEqualTo(EXPECTED_RGB_565_BYTES);
  }

  @Test
  public void loadJpegResource_noFallback_withPreferRgb565_returnsRgb565Config() {
    initGlideForImageDecoder(
        ResourceIds.raw.canonical, /* enableRgb565Fix= */ false, /* poolBitmap= */ null);

    Bitmap bitmap = loadPreferRgb565Bitmap(DEFAULT_WIDTH, DEFAULT_HEIGHT);

    assertThat(bitmap).isNotNull();
    assertThat(bitmap.getConfig()).isEqualTo(Bitmap.Config.RGB_565);
  }

  /* Verify no allocation size bug with ImageDecoder enabled and RGB_565 */
  @Test
  public void loadJpegResource_noFallback_withPreferRgb565_sizedForRgb565() {
    initGlideForImageDecoder(
        ResourceIds.raw.canonical, /* enableRgb565Fix= */ false, /* poolBitmap= */ null);

    Bitmap bitmap = loadPreferRgb565Bitmap(DEFAULT_WIDTH, DEFAULT_HEIGHT);

    // Verify 2 bytes allocated per pixel (RGB_565) natively handled by ImageDecoder
    assertThat(bitmap).isNotNull();
    assertThat(Util.getBitmapByteSize(bitmap)).isEqualTo(EXPECTED_RGB_565_BYTES);
  }

  private void initGlideForImageDecoder(
      int resourceId, boolean enableRgb565Fix, @Nullable Bitmap poolBitmap) {
    BitmapPool pool = new LruBitmapPool(BITMAP_POOL_SIZE_BYTES);

    if (poolBitmap != null) {
      pool.put(poolBitmap);
    }

    Glide.init(
        context,
        new GlideBuilder()
            .setBitmapPool(pool)
            .experimentalSetEnableRgb565DownsamplerFix(enableRgb565Fix)
            .setImageDecoderEnabledForBitmaps(true)
            .setUseArrayPoolForImageDecoderByteBufferAllocation(true));

    Glide.get(context)
        .getRegistry()
        .prepend(Object.class, InputStream.class, new FakeStreamModelLoader<>(context, resourceId));
  }

  private Bitmap loadPreferRgb565Bitmap(int width, int height) {
    return concurrency.get(
        Glide.with(context)
            .asBitmap()
            .format(DecodeFormat.PREFER_RGB_565)
            .load(new Object())
            .override(width, height)
            .submit());
  }
}
