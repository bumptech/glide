package com.bumptech.glide;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.test.ConcurrencyHelper;
import com.bumptech.glide.test.GlideApp;
import com.bumptech.glide.test.MockModelLoader;
import com.bumptech.glide.test.ResourceIds;
import com.bumptech.glide.test.TearDownGlide;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AsFileTest {
  private static final String URL = "https://imgs.xkcd.com/comics/mc_hammer_age.png";
  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();
  private final ConcurrencyHelper concurrency = new ConcurrencyHelper();
  private final Context context = ApplicationProvider.getApplicationContext();

  @Before
  public void setUp() {
    MockModelLoader.mock(URL, getData());
  }

  @Test
  public void asFile_withUrl_succeeds() {
    File file = concurrency.get(GlideApp.with(context).asFile().load(URL).submit());
    assertThat(file).isNotNull();
  }

  @Test
  public void asFile_withUrlAndDiskCacheStrategyAutomatic_succeeds() {
    File file =
        concurrency.get(
            GlideApp.with(context)
                .asFile()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .load(URL)
                .submit());
    assertThat(file).isNotNull();
  }

  @Test
  public void asFile_withUrlAndDiskCacheStrategyData_succeeds() {
    File file =
        concurrency.get(
            GlideApp.with(context)
                .asFile()
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .load(URL)
                .submit());
    assertThat(file).isNotNull();
  }

  @Test
  public void asFile_withUrlAndDiskCacheStrategyResource_fails() {
    try {
      concurrency.get(
          GlideApp.with(context)
              .asFile()
              .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
              .load(URL)
              .submit());
      fail();
    } catch (RuntimeException e) {
      // expected.
    }
  }

  @Test
  public void asFile_withUrlAndDiskCacheStrategyAll_fails() {
    try {
      concurrency.get(
          GlideApp.with(context)
              .asFile()
              .diskCacheStrategy(DiskCacheStrategy.ALL)
              .load(URL)
              .submit());
      fail();
    } catch (RuntimeException e) {
      // Expected.
    }
  }

  private InputStream getData() {
    InputStream is = null;
    try {
      is = context.getResources().openRawResource(ResourceIds.raw.canonical);
      byte[] buffer = new byte[1024 * 1024];
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      int read;
      while ((read = is.read(buffer)) != -1) {
        outputStream.write(buffer, 0, read);
      }
      byte[] data = outputStream.toByteArray();
      return new ByteArrayInputStream(data);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          // Ignored.
        }
      }
    }
  }
}
