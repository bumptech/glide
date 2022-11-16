package com.bumptech.glide;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.ColorSpace;
import android.graphics.ColorSpace.Named;
import android.os.Build;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.load.resource.bitmap.Downsampler;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.test.GlideApp;
import com.bumptech.glide.test.ResourceIds;
import com.bumptech.glide.testutil.ConcurrencyHelper;
import com.bumptech.glide.testutil.TearDownGlide;
import java.io.ByteArrayOutputStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WideGamutTest {
  @Rule public final TestRule tearDownGlide = new TearDownGlide();
  private final ConcurrencyHelper concurrency = new ConcurrencyHelper();
  private final Context context = ApplicationProvider.getApplicationContext();

  @Before
  public void setUp() {
    assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O);
  }

  @Test
  public void load_withWideGamutImage_returnsWideGamutBitmap() {
    Bitmap bitmap =
        concurrency.get(
            Glide.with(context).asBitmap().load(ResourceIds.raw.webkit_logo_p3).submit());
    assertThat(bitmap.getConfig()).isEqualTo(Bitmap.Config.RGBA_F16);
  }

  @Test
  public void load_withWideGamutImage_bitmapInPoolWithSizeAndConfig_usesBitmapFromPool() {
    int bitmapDimension = 1000;
    Glide.init(
        context,
        new GlideBuilder()
            .setBitmapPool(new LruBitmapPool(bitmapDimension * bitmapDimension * 8 * 4)));
    Bitmap expected = Bitmap.createBitmap(bitmapDimension, bitmapDimension, Bitmap.Config.RGBA_F16);

    Glide.get(context).getBitmapPool().put(expected);

    Bitmap bitmap =
        concurrency.get(
            Glide.with(context).asBitmap().load(ResourceIds.raw.webkit_logo_p3).submit());
    assertThat(bitmap).isSameInstanceAs(expected);
  }

  // TODO: Even with hardware allowed, we get a wide F16. Attempting to decode the resource with
  // preferred config set to hardware fails with:
  // "D/skia    (10312): --- Failed to allocate a hardware bitmap"
  @Test
  public void load_withWideGamutImage_hardwareAllowed_returnsDecodedBitmap() {
    Bitmap bitmap =
        concurrency.get(
            GlideApp.with(context)
                .asBitmap()
                .load(ResourceIds.raw.webkit_logo_p3)
                .set(Downsampler.ALLOW_HARDWARE_CONFIG, true)
                .submit());
    assertThat(bitmap).isNotNull();
  }

  @Test
  public void load_withEncodedPngWideGamutImage_decodesWideGamut() {
    Bitmap toCompress =
        Bitmap.createBitmap(
            100, 100, Bitmap.Config.RGBA_F16, /* hasAlpha= */ true, ColorSpace.get(Named.DCI_P3));

    byte[] data = asPng(toCompress);

    Bitmap bitmap = concurrency.get(Glide.with(context).asBitmap().load(data).submit());
    assertThat(bitmap.getConfig()).isEqualTo(Bitmap.Config.RGBA_F16);
  }

  @Test
  public void load_withEncodedJpegWideGamutImage_decodesArgb8888() {
    // TODO(b/71430152): Figure out whether or not this is supposed to pass in API 26 and fail in
    // API 27.
    assumeTrue(Build.VERSION.SDK_INT != Build.VERSION_CODES.O_MR1);
    Bitmap toCompress =
        Bitmap.createBitmap(
            100, 100, Bitmap.Config.RGBA_F16, /* hasAlpha= */ true, ColorSpace.get(Named.DCI_P3));

    byte[] data = asJpeg(toCompress);

    Bitmap bitmap = concurrency.get(Glide.with(context).asBitmap().load(data).submit());
    assertThat(bitmap.getConfig()).isEqualTo(Bitmap.Config.ARGB_8888);
  }

  @Test
  public void load_withEncodedWebpWideGamutImage_decodesArgb8888() {
    Bitmap toCompress =
        Bitmap.createBitmap(
            100, 100, Bitmap.Config.RGBA_F16, /* hasAlpha= */ true, ColorSpace.get(Named.DCI_P3));

    byte[] data = asWebp(toCompress);

    Bitmap bitmap = concurrency.get(Glide.with(context).asBitmap().load(data).submit());
    assertThat(bitmap.getConfig()).isEqualTo(Bitmap.Config.ARGB_8888);
  }

  @Test
  public void load_withSmallerWideGamutInPool_decodesBitmap() {
    BitmapPool bitmapPool = Glide.get(context).getBitmapPool();
    Bitmap toPut = Bitmap.createBitmap(300, 298, Config.RGBA_F16);
    bitmapPool.put(toPut);
    // Add a second Bitmap to account for the InputStream decode.
    bitmapPool.put(Bitmap.createBitmap(toPut));

    Bitmap wideGamut = Bitmap.createBitmap(300, 300, Config.RGBA_F16);
    byte[] data = asPng(wideGamut);
    Bitmap bitmap = concurrency.get(Glide.with(context).asBitmap().load(data).submit());
    assertThat(bitmap).isNotNull();
  }

  @Test
  public void circleCrop_withWideGamutBitmap_producesWideGamutBitmap() {
    Bitmap bitmap = Bitmap.createBitmap(100, 100, Config.RGBA_F16);
    byte[] data = asPng(bitmap);

    Bitmap result =
        concurrency.get(GlideApp.with(context).asBitmap().load(data).circleCrop().submit());
    assertThat(result).isNotNull();
    assertThat(result.getConfig()).isEqualTo(Config.RGBA_F16);
  }

  @Test
  public void roundedCorners_withWideGamutBitmap_producesWideGamutBitmap() {
    Bitmap bitmap = Bitmap.createBitmap(100, 100, Config.RGBA_F16);
    byte[] data = asPng(bitmap);

    Bitmap result =
        concurrency.get(
            GlideApp.with(context)
                .asBitmap()
                .load(data)
                .transform(new RoundedCorners(/* roundingRadius= */ 10))
                .submit());
    assertThat(result).isNotNull();
    assertThat(result.getConfig()).isEqualTo(Config.RGBA_F16);
  }

  @Test
  public void loadWideGamutImage_withArgb888OfSufficientSizeInPool_usesArgb8888Bitmap() {
    Bitmap wideGamut = Bitmap.createBitmap(100, 50, Bitmap.Config.RGBA_F16);
    byte[] data = asPng(wideGamut);

    Bitmap argb8888 = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    Glide.init(
        context,
        new GlideBuilder()
            .setBitmapPool(new LruBitmapPool(wideGamut.getAllocationByteCount() * 5)));
    Glide.get(context).getBitmapPool().put(argb8888);

    Bitmap result = concurrency.get(Glide.with(context).asBitmap().load(data).submit());

    assertThat(result).isSameInstanceAs(argb8888);
  }

  private static byte[] asJpeg(Bitmap bitmap) {
    return toByteArray(bitmap, CompressFormat.JPEG);
  }

  private static byte[] asPng(Bitmap bitmap) {
    return toByteArray(bitmap, CompressFormat.PNG);
  }

  private static byte[] asWebp(Bitmap bitmap) {
    return toByteArray(bitmap, CompressFormat.WEBP);
  }

  private static byte[] toByteArray(Bitmap bitmap, CompressFormat format) {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    assertThat(bitmap.compress(format, 100, os)).isTrue();
    return os.toByteArray();
  }
}
