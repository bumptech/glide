package com.bumptech.glide;


import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AsFileTest {
  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();
  private final ConcurrencyHelper concurrency = new ConcurrencyHelper();
  private final Context context = InstrumentationRegistry.getTargetContext();

  @Test
  public void asFile_withUrl_succeeds() {
    String url = "https://www.w3schools.com/howto/img_fjords.jpg";

    MockModelLoader.mock(url, getData());

    File file =
        concurrency.get(
            GlideApp.with(context)
                .asFile()
                .load("https://www.w3schools.com/howto/img_fjords.jpg")
                .submit());
    assertThat(file).isNotNull();
  }

  @Test
  public void asFile_withUrlAndDiskCacheStrategyData_succeeds() {
    String url = "https://www.w3schools.com/howto/img_fjords.jpg";

    MockModelLoader.mock(url, getData());

    File file =
        concurrency.get(
            GlideApp.with(context)
                .asFile()
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .load("https://www.w3schools.com/howto/img_fjords.jpg")
                .submit());
    assertThat(file).isNotNull();
  }

  @Test
  public void asFile_withUrlAndDiskCacheStrategyResource_fails() {
    String url = "https://www.w3schools.com/howto/img_fjords.jpg";

    MockModelLoader.mock(url, getData());

    try {
      concurrency.get(
          GlideApp.with(context)
              .asFile()
              .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
              .load("https://www.w3schools.com/howto/img_fjords.jpg")
              .submit());
      fail();
    } catch (RuntimeException e) {
      // expected.
    }
  }

  @Test
  public void asFile_withUrlAndDiskCacheStrategyAll_fails() {
    String url = "https://www.w3schools.com/howto/img_fjords.jpg";

    MockModelLoader.mock(url, getData());

    try {
      concurrency.get(
          GlideApp.with(context)
              .asFile()
              .diskCacheStrategy(DiskCacheStrategy.ALL)
              .load("https://www.w3schools.com/howto/img_fjords.jpg")
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
