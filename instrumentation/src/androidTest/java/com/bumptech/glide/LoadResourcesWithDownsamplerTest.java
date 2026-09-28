package com.bumptech.glide;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.os.Build;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.load.resource.bitmap.Downsampler;
import com.bumptech.glide.test.FakeStreamModelLoader;
import com.bumptech.glide.test.GlideApp;
import com.bumptech.glide.test.ResourceIds;
import com.bumptech.glide.testutil.ConcurrencyHelper;
import com.bumptech.glide.testutil.TearDownGlide;
import com.bumptech.glide.util.Util;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for loading resources with {@link Downsampler}.
 *
 * <p>These tests verify:
 *
 * <ul>
 *   <li>A workaround for an Android framework bug on API 26, where {@link
 *       android.graphics.BitmapFactory} sets {@link
 *       android.graphics.BitmapFactory.Options#outConfig} to {@code null} when {@link
 *       android.graphics.BitmapFactory.Options#inJustDecodeBounds} is {@code true}. Fallback
 *       loaders that normally mask this failure are bypassed to test {@link Downsampler} directly.
 *   <li>A fix for a {@link Downsampler} bug on API 26+, where {@link Bitmap.Config#RGB_565} bitmaps
 *       were allocated with 4 bytes per pixel instead of 2.
 * </ul>
 */
@RunWith(AndroidJUnit4.class)
public class LoadResourcesWithDownsamplerTest {
  private static final int BITMAP_POOL_SIZE_BYTES = 10 * 1024 * 1024;
  private static final int DEFAULT_WIDTH = 320;
  private static final int DEFAULT_HEIGHT = 240;
  private static final int EXPECTED_RGB_565_BYTES = DEFAULT_WIDTH * DEFAULT_HEIGHT * 2;
  private static final int EXPECTED_ARGB_8888_BYTES = DEFAULT_WIDTH * DEFAULT_HEIGHT * 4;
  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();
  private final ConcurrencyHelper concurrency = new ConcurrencyHelper();
  private final Context context = ApplicationProvider.getApplicationContext();

  @Test
  public void loadJpegResource_withNoOtherLoaders_decodesResource() {
    Glide.get(context)
        .getRegistry()
        .prepend(
            Object.class,
            InputStream.class,
            new FakeStreamModelLoader<>(context, ResourceIds.raw.canonical));

    Bitmap bitmap = concurrency.get(Glide.with(context).asBitmap().load(new Object()).submit());
    assertThat(bitmap).isNotNull();
  }

  @Test
  public void loadWideGamutJpegResource_withNoOtherLoaders_decodesWideGamutBitmap() {
    assumeTrue(
        "Wide gamut is only available on O+", Build.VERSION.SDK_INT >= Build.VERSION_CODES.O);
    Glide.get(context)
        .getRegistry()
        .prepend(
            Object.class,
            InputStream.class,
            new FakeStreamModelLoader<>(context, ResourceIds.raw.webkit_logo_p3));

    Bitmap bitmap = concurrency.get(Glide.with(context).asBitmap().load(new Object()).submit());
    assertThat(bitmap).isNotNull();
    assertThat(bitmap.getConfig()).isEqualTo(Bitmap.Config.RGBA_F16);

    // The exact value here depends on the emulator / device we're running on. On Pixel devices and
    // emulators it'll return DISPLAY_P3. On 'generic' emulators and some other devices, it'll
    // return LINEAR_EXTENDED_SRGB. It's unclear how else we can assert correctly based on the
    // device type, so I've just left this is isAnyOf for now.
    assertThat(bitmap.getColorSpace())
        .isAnyOf(
            ColorSpace.get(ColorSpace.Named.DISPLAY_P3),
            ColorSpace.get(ColorSpace.Named.LINEAR_EXTENDED_SRGB));
  }

  @Test
  public void loadOpaquePngResource_withNoOtherLoaders_decodesResource() {
    Glide.get(context)
        .getRegistry()
        .prepend(
            Object.class,
            InputStream.class,
            new FakeStreamModelLoader<>(context, ResourceIds.raw.canonical_png));

    Bitmap bitmap = concurrency.get(Glide.with(context).asBitmap().load(new Object()).submit());
    assertThat(bitmap).isNotNull();
  }

  @Test
  public void loadTransparentPngResource_withNoOtherLoaders_decodesResource() {
    Glide.get(context)
        .getRegistry()
        .prepend(
            Object.class,
            InputStream.class,
            new FakeStreamModelLoader<>(context, ResourceIds.raw.canonical_transparent_png));

    Bitmap bitmap = concurrency.get(Glide.with(context).asBitmap().load(new Object()).submit());
    assertThat(bitmap).isNotNull();
  }

  @Test
  public void loadTransparentGifResource_withNoOtherLoaders_decodesResource() {
    Glide.get(context)
        .getRegistry()
        .prepend(
            Object.class,
            InputStream.class,
            new FakeStreamModelLoader<>(context, ResourceIds.raw.transparent_gif));

    Bitmap bitmap = concurrency.get(Glide.with(context).asBitmap().load(new Object()).submit());
    assertThat(bitmap).isNotNull();
  }

  @Test
  public void loadTransparentGifResource_asHardware_withNoOtherLoaders_decodesResource()
      throws InterruptedException {
    assumeTrue(
        "Hardware Bitmaps are only supported on P+",
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P);
    // enableHardwareBitmaps must be called on the main thread.
    final CountDownLatch latch = new CountDownLatch(1);
    Util.postOnUiThread(
        new Runnable() {
          @Override
          public void run() {
            Glide.enableHardwareBitmaps();
            latch.countDown();
          }
        });
    latch.await(5, TimeUnit.SECONDS);

    Glide.get(context)
        .getRegistry()
        .prepend(
            Object.class,
            InputStream.class,
            new FakeStreamModelLoader<>(context, ResourceIds.raw.transparent_gif));

    Bitmap bitmap =
        concurrency.get(
            GlideApp.with(context)
                .asBitmap()
                .set(Downsampler.ALLOW_HARDWARE_CONFIG, true)
                .format(DecodeFormat.PREFER_ARGB_8888)
                .load(new Object())
                .submit());
    assertThat(bitmap).isNotNull();
    assertThat(bitmap.getConfig()).isEqualTo(Bitmap.Config.HARDWARE);
  }

  @Test
  public void loadTransparentGifResource_withNoOtherLoaders_fromBytes_decodesResource() {
    byte[] data = FakeStreamModelLoader.getBytes(context, ResourceIds.raw.transparent_gif);
    Bitmap bitmap = concurrency.get(Glide.with(context).asBitmap().load(data).submit());
    assertThat(bitmap).isNotNull();
  }

  @Test
  public void loadOpaqueGifResource_withNoOtherLoaders_decodesResource() {
    Glide.get(context)
        .getRegistry()
        .prepend(
            Object.class,
            InputStream.class,
            new FakeStreamModelLoader<>(context, ResourceIds.raw.opaque_gif));

    Bitmap bitmap = concurrency.get(Glide.with(context).asBitmap().load(new Object()).submit());
    assertThat(bitmap).isNotNull();
  }

  @Test
  public void loadOpaqueGifResource_asBytes_decodesResource() {
    byte[] data = FakeStreamModelLoader.getBytes(context, ResourceIds.raw.opaque_gif);
    Bitmap bitmap = concurrency.get(Glide.with(context).asBitmap().load(data).submit());
    assertThat(bitmap).isNotNull();
  }

  @Test
  public void loadOpaqueGifResource_asHardware_withNoOtherLoaders_decodesResource() {
    assumeTrue(
        "Hardware Bitmaps are only supported on P+",
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P);

    Glide.get(context)
        .getRegistry()
        .prepend(
            Object.class,
            InputStream.class,
            new FakeStreamModelLoader<>(context, ResourceIds.raw.opaque_gif));

    Bitmap bitmap =
        concurrency.get(
            GlideApp.with(context)
                .asBitmap()
                // Allow HARDWARE Bitmaps.
                .format(DecodeFormat.PREFER_ARGB_8888)
                .load(new Object())
                .submit());
    assertThat(bitmap).isNotNull();
  }

  @Test
  public void loadJpegResource_withPreferRgb565_fixEnabled_returnsRgb565Config() {
    Bitmap expectedBitmap =
        Bitmap.createBitmap(DEFAULT_WIDTH, DEFAULT_HEIGHT, Bitmap.Config.RGB_565);
    initGlideWithPool(ResourceIds.raw.canonical, /* enableRgb565Fix= */ true, expectedBitmap);

    Bitmap bitmap = loadPreferRgb565Bitmap(DEFAULT_WIDTH, DEFAULT_HEIGHT);

    assertThat(bitmap).isSameInstanceAs(expectedBitmap);
    assertThat(bitmap.getConfig()).isEqualTo(Bitmap.Config.RGB_565);
  }

  @Test
  public void loadJpegResource_withPreferRgb565_fixEnabled_sizedForRgb565() {
    initGlideWithPool(
        ResourceIds.raw.canonical, /* enableRgb565Fix= */ true, /* expectedBitmap= */ null);

    Bitmap bitmap = loadPreferRgb565Bitmap(DEFAULT_WIDTH, DEFAULT_HEIGHT);

    // Verify 2 bytes allocated per pixel (RGB_565) not 4 (ARGB_8888)
    assertThat(bitmap).isNotNull();
    assertThat(Util.getBitmapByteSize(bitmap)).isEqualTo(EXPECTED_RGB_565_BYTES);
  }

  @Test
  public void loadJpegResource_withPreferRgb565_fixDisabled_sizedForArgb8888() {
    assumeTrue(
        "RGB_565 oversized-allocation bug is only present on O+",
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O);
    initGlideWithPool(
        ResourceIds.raw.canonical, /* enableRgb565Fix= */ false, /* expectedBitmap= */ null);

    Bitmap bitmap = loadPreferRgb565Bitmap(DEFAULT_WIDTH, DEFAULT_HEIGHT);

    // Verify bad behavior: 4 bytes allocated per pixel (ARGB_8888)
    assertThat(bitmap).isNotNull();
    assertThat(Util.getBitmapByteSize(bitmap)).isEqualTo(EXPECTED_ARGB_8888_BYTES);
  }

  @Test
  public void loadTransparentPngResource_withPreferRgb565_fixEnabled_returnsArgb8888Config() {
    Bitmap expectedBitmap =
        Bitmap.createBitmap(DEFAULT_WIDTH, DEFAULT_HEIGHT, Bitmap.Config.ARGB_8888);
    initGlideWithPool(
        ResourceIds.raw.canonical_transparent_png, /* enableRgb565Fix= */ true, expectedBitmap);

    Bitmap bitmap = loadPreferRgb565Bitmap(DEFAULT_WIDTH, DEFAULT_HEIGHT);

    assertThat(bitmap).isSameInstanceAs(expectedBitmap);
    assertThat(bitmap.getConfig()).isEqualTo(Bitmap.Config.ARGB_8888);
  }

  @Test
  public void loadTransparentPngResource_withPreferRgb565_fixEnabled_sizedForArgb8888() {
    initGlideWithPool(
        ResourceIds.raw.canonical_transparent_png,
        /* enableRgb565Fix= */ true,
        /* expectedBitmap= */ null);

    Bitmap bitmap = loadPreferRgb565Bitmap(DEFAULT_WIDTH, DEFAULT_HEIGHT);

    // Verify 4 bytes allocated per pixel (ARGB_8888)
    assertThat(bitmap).isNotNull();
    assertThat(Util.getBitmapByteSize(bitmap)).isEqualTo(EXPECTED_ARGB_8888_BYTES);
  }

  private void initGlideWithPool(
      int resourceId, boolean enableRgb565Fix, @Nullable Bitmap expectedBitmap) {
    BitmapPool pool = new LruBitmapPool(BITMAP_POOL_SIZE_BYTES);
    Glide.init(
        context,
        new GlideBuilder()
            .setBitmapPool(pool)
            .experimentalSetEnableRgb565DownsamplerFix(enableRgb565Fix));

    Glide.get(context)
        .getRegistry()
        .prepend(Object.class, InputStream.class, new FakeStreamModelLoader<>(context, resourceId));

    if (expectedBitmap != null) {
      pool.put(expectedBitmap);
    }
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
