package com.bumptech.glide.load.resource.bitmap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import android.graphics.Bitmap;

import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class DownsamplerTest {
  private File tempFile;
  private HashMap<String, Object> options;

  @Before
  public void setUp() throws Exception {
    File cacheDir = RuntimeEnvironment.application.getCacheDir();
    cacheDir.mkdir();
    tempFile = new File(cacheDir, "temp");
    options = new HashMap<>();
  }

  @After
  public void tearDown() throws Exception {
    tempFile.delete();
  }

  @Test
  public void testAlwaysArgb8888() throws FileNotFoundException {
    Bitmap rgb565 = Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565);
    compressBitmap(rgb565, Bitmap.CompressFormat.JPEG);
    Downsampler downsampler = Downsampler.AT_LEAST;
    InputStream is = new FileInputStream(tempFile);
    options.put(Downsampler.KEY_DECODE_FORMAT, DecodeFormat.ALWAYS_ARGB_8888);
    try {
      Bitmap result = downsampler.decode(is, mock(BitmapPool.class), 100, 100, options);
      assertEquals(Bitmap.Config.ARGB_8888, result.getConfig());
    } finally {
      try {
        is.close();
      } catch (IOException e) {
        // Do nothing.
      }
    }
  }

  @Test
  public void testPreferRgb565() throws FileNotFoundException {
    Bitmap rgb565 = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    compressBitmap(rgb565, Bitmap.CompressFormat.JPEG);
    Downsampler downsampler = Downsampler.AT_LEAST;
    InputStream is = new FileInputStream(tempFile);
    options.put(Downsampler.KEY_DECODE_FORMAT, DecodeFormat.PREFER_RGB_565);
    try {
      Bitmap result = downsampler.decode(is, mock(BitmapPool.class), 100, 100, options);
      assertEquals(Bitmap.Config.RGB_565, result.getConfig());
    } finally {
      try {
        is.close();
      } catch (IOException e) {
        // Do nothing.
      }
    }
  }

  private void compressBitmap(Bitmap bitmap, Bitmap.CompressFormat compressFormat)
      throws FileNotFoundException {
    OutputStream os = null;
    try {
      os = new BufferedOutputStream(new FileOutputStream(tempFile));
      bitmap.compress(compressFormat, 100, os);
    } finally {
      if (os != null) {
        try {
          os.close();
        } catch (IOException e) {
          // Do nothing.
        }
      }
    }

  }
}
