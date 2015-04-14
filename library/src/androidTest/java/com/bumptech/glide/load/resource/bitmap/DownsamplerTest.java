package com.bumptech.glide.load.resource.bitmap;

import static org.junit.Assert.assertEquals;

import android.graphics.Bitmap;

import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class DownsamplerTest {
  @Mock private BitmapPool bitmapPool;
  private Downsampler downsampler;
  private Options options;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    options = new Options();
    downsampler = new Downsampler(RuntimeEnvironment.application.getResources().getDisplayMetrics(),
        bitmapPool);
  }

  @Test
  public void testAlwaysArgb8888() throws IOException {
    Bitmap rgb565 = Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565);
    InputStream stream = compressBitmap(rgb565, Bitmap.CompressFormat.JPEG);

    options.set(Downsampler.DECODE_FORMAT, DecodeFormat.PREFER_ARGB_8888);
    Resource<Bitmap> result = downsampler.decode(stream, 100, 100, options);
    assertEquals(Bitmap.Config.ARGB_8888, result.get().getConfig());
  }

  @Test
  public void testPreferRgb565() throws IOException {
    Bitmap rgb565 = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    InputStream stream = compressBitmap(rgb565, Bitmap.CompressFormat.JPEG);

    options.set(Downsampler.DECODE_FORMAT, DecodeFormat.PREFER_RGB_565);
    Resource<Bitmap> result = downsampler.decode(stream, 100, 100, options);
    assertEquals(Bitmap.Config.RGB_565, result.get().getConfig());
  }

  private InputStream compressBitmap(Bitmap bitmap, Bitmap.CompressFormat compressFormat)
      throws FileNotFoundException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    bitmap.compress(compressFormat, 100, os);
    return new ByteArrayInputStream(os.toByteArray());
  }
}
