package com.bumptech.glide.load.resource.bitmap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import android.graphics.Bitmap;

import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class DownsamplerTest {
  private HashMap<String, Object> options;

  @Before
  public void setUp() throws Exception {
    options = new HashMap<>();
  }

  @Test
  public void testAlwaysArgb8888() throws IOException {
    Bitmap rgb565 = Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565);
    InputStream stream = compressBitmap(rgb565, Bitmap.CompressFormat.JPEG);

    Downsampler downsampler = Downsampler.AT_LEAST;
    options.put(Downsampler.KEY_DECODE_FORMAT, DecodeFormat.PREFER_ARGB_8888);
    Bitmap result = downsampler.decode(stream, mock(BitmapPool.class), 100, 100, options);
    assertEquals(Bitmap.Config.ARGB_8888, result.getConfig());
  }

  @Test
  public void testPreferRgb565() throws IOException {
    Bitmap rgb565 = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    InputStream stream = compressBitmap(rgb565, Bitmap.CompressFormat.JPEG);

    Downsampler downsampler = Downsampler.AT_LEAST;
    options.put(Downsampler.KEY_DECODE_FORMAT, DecodeFormat.PREFER_RGB_565);
    Bitmap result = downsampler.decode(stream, mock(BitmapPool.class), 100, 100, options);
    assertEquals(Bitmap.Config.RGB_565, result.getConfig());
  }


  private InputStream compressBitmap(Bitmap bitmap, Bitmap.CompressFormat compressFormat)
      throws FileNotFoundException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    bitmap.compress(compressFormat, 100, os);
    return new ByteArrayInputStream(os.toByteArray());
  }
}
