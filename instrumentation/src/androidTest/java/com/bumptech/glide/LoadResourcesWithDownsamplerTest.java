package com.bumptech.glide;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorSpace;
import android.os.Build;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.load.DecodeFormat;
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * On API 26, decoding a variety of different images can cause {@link BitmapFactory} with {@link
 * BitmapFactory.Options#inJustDecodeBounds} set to {@code true} to set {@link
 * BitmapFactory.Options#outConfig} to null instead of a valid value, even though the image can be
 * decoded successfully. Glide can mask these failures by decoding some image sources (notably
 * including resource ids) using other data types and decoders.
 *
 * <p>This test ensures that we've worked around the framework issue by loading a variety of images
 * and image types without the normal fallback behavior.
 */
@RunWith(AndroidJUnit4.class)
public class LoadResourcesWithDownsamplerTest {
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
}
