package com.bumptech.glide;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.ColorSpace;
import android.graphics.ColorSpace.Named;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.test.ConcurrencyHelper;
import com.bumptech.glide.test.GlideApp;
import com.bumptech.glide.test.ResourceIds;
import com.bumptech.glide.test.TearDownGlide;
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
  private final Context context = InstrumentationRegistry.getTargetContext();

  @Before
  public void setUp() {
    assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O);
  }

  @Test
  public void load_withWideGamutImage_returnsWideGamutBitmap() {
    Bitmap bitmap =
        concurrency.get(
            Glide.with(context)
                .asBitmap()
                .load(ResourceIds.raw.webkit_logo_p3)
                .submit());
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

    Glide.get(context)
        .getBitmapPool()
        .put(expected);

    Bitmap bitmap =
        concurrency.get(
            Glide.with(context)
                .asBitmap()
                .load(ResourceIds.raw.webkit_logo_p3)
                .submit());
    assertThat(bitmap).isSameAs(expected);
  }

  @Test
  public void load_withWideGamutImage_hardwareAllowed_returnsHardwareBitmap() {
    Bitmap bitmap =
        concurrency.get(
            GlideApp.with(context)
                .asBitmap()
                .format(DecodeFormat.PREFER_ARGB_8888)
                .load(ResourceIds.raw.webkit_logo_p3)
                .submit());
    assertThat(bitmap.getConfig()).isEqualTo(Bitmap.Config.HARDWARE);
  }

  @Test
  public void load_withEncodedPngWideGamutImage_decodesWideGamut() {
    Bitmap toCompress =
        Bitmap.createBitmap(
            100, 100, Bitmap.Config.RGBA_F16, /*hasAlpha=*/ true, ColorSpace.get(Named.DCI_P3));

    ByteArrayOutputStream os = new ByteArrayOutputStream();
    assertThat(toCompress.compress(CompressFormat.PNG, 100, os)).isTrue();
    byte[] data = os.toByteArray();

    Bitmap bitmap =
        concurrency.get(
            Glide.with(context)
                .asBitmap()
                .load(data)
                .submit());
    assertThat(bitmap.getConfig()).isEqualTo(Bitmap.Config.RGBA_F16);
  }

  @Test
  public void load_withEncodedJpegWideGamutImage_decodesArgb8888() {
    // TODO(b/71430152): Figure out whether or not this is supposed to pass in API 26 and fail in
    // API 27.
    assumeTrue(Build.VERSION.SDK_INT != Build.VERSION_CODES.O_MR1);
    Bitmap toCompress =
        Bitmap.createBitmap(
            100, 100, Bitmap.Config.RGBA_F16, /*hasAlpha=*/ true, ColorSpace.get(Named.DCI_P3));

    ByteArrayOutputStream os = new ByteArrayOutputStream();
    assertThat(toCompress.compress(CompressFormat.JPEG, 100, os)).isTrue();
    byte[] data = os.toByteArray();

    Bitmap bitmap =
        concurrency.get(
            Glide.with(context)
                .asBitmap()
                .load(data)
                .submit());
    assertThat(bitmap.getConfig()).isEqualTo(Bitmap.Config.ARGB_8888);
  }

  @Test
  public void load_withEncodedWebpWideGamutImage_decodesArgb8888() {
    Bitmap toCompress =
        Bitmap.createBitmap(
            100, 100, Bitmap.Config.RGBA_F16, /*hasAlpha=*/ true, ColorSpace.get(Named.DCI_P3));

    ByteArrayOutputStream os = new ByteArrayOutputStream();
    assertThat(toCompress.compress(CompressFormat.WEBP, 100, os)).isTrue();
    byte[] data = os.toByteArray();

    Bitmap bitmap =
        concurrency.get(
            Glide.with(context)
                .asBitmap()
                .load(data)
                .submit());
    assertThat(bitmap.getConfig()).isEqualTo(Bitmap.Config.ARGB_8888);
  }
}
