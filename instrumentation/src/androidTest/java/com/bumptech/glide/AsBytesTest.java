package com.bumptech.glide;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.test.ConcurrencyHelper;
import com.bumptech.glide.test.GlideApp;
import com.bumptech.glide.test.ResourceIds;
import com.bumptech.glide.test.TearDownGlide;
import com.google.common.io.ByteStreams;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class AsBytesTest {
  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();
  private final ConcurrencyHelper concurrency = new ConcurrencyHelper();

  private Context context;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    context = ApplicationProvider.getApplicationContext();
  }

  @Test
  public void loadImageResourceId_asBytes_providesBytesOfBitmap() {
    byte[] data =
        concurrency.get(
            Glide.with(context).as(byte[].class).load(ResourceIds.raw.canonical).submit());
    assertThat(data).isNotNull();
    assertThat(BitmapFactory.decodeByteArray(data, 0, data.length)).isNotNull();
  }

  @Test
  public void loadBitmap_asBytes_providesBytesOfBitmap() {
    Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), ResourceIds.raw.canonical);
    byte[] data = concurrency.get(Glide.with(context).as(byte[].class).load(bitmap).submit());

    assertThat(data).isNotNull();
    assertThat(BitmapFactory.decodeByteArray(data, 0, data.length)).isNotNull();
  }

  @Test
  public void loadBitmapDrawable_asBytes_providesBytesOfBitmap() {
    Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), ResourceIds.raw.canonical);
    byte[] data =
        concurrency.get(
            Glide.with(context)
                .as(byte[].class)
                .load(new BitmapDrawable(context.getResources(), bitmap))
                .submit());

    assertThat(data).isNotNull();
    assertThat(BitmapFactory.decodeByteArray(data, 0, data.length)).isNotNull();
  }

  @Test
  public void loadVideoResourceId_asBytes_providesBytesOfFrame() {
    byte[] data =
        concurrency.get(Glide.with(context).as(byte[].class).load(ResourceIds.raw.video).submit());

    assertThat(data).isNotNull();
    assertThat(BitmapFactory.decodeByteArray(data, 0, data.length)).isNotNull();
  }

  @Test
  public void loadVideoResourceId_asBytes_withFrameTime_providesBytesOfFrame() {
    byte[] data =
        concurrency.get(
            GlideApp.with(context)
                .as(byte[].class)
                .load(ResourceIds.raw.video)
                .frame(TimeUnit.SECONDS.toMicros(1))
                .submit());

    assertThat(data).isNotNull();
    assertThat(BitmapFactory.decodeByteArray(data, 0, data.length)).isNotNull();
  }

  @Test
  public void loadVideoFile_asBytes_providesByteOfFrame() throws IOException {
    byte[] data =
        concurrency.get(Glide.with(context).as(byte[].class).load(writeVideoToFile()).submit());

    assertThat(data).isNotNull();
    assertThat(BitmapFactory.decodeByteArray(data, 0, data.length)).isNotNull();
  }

  @Test
  public void loadVideoFile_asBytes_withFrameTime_providesByteOfFrame() throws IOException {
    byte[] data =
        concurrency.get(
            GlideApp.with(context)
                .as(byte[].class)
                .load(writeVideoToFile())
                .frame(TimeUnit.SECONDS.toMicros(1))
                .submit());

    assertThat(data).isNotNull();
    assertThat(BitmapFactory.decodeByteArray(data, 0, data.length)).isNotNull();
  }

  @Test
  public void loadVideoFilePath_asBytes_providesByteOfFrame() throws IOException {
    byte[] data =
        concurrency.get(
            Glide.with(context)
                .as(byte[].class)
                .load(writeVideoToFile().getAbsolutePath())
                .submit());

    assertThat(data).isNotNull();
    assertThat(BitmapFactory.decodeByteArray(data, 0, data.length)).isNotNull();
  }

  @Test
  public void loadVideoFilePath_asBytes_withFrameTime_providesByteOfFrame() throws IOException {
    byte[] data =
        concurrency.get(
            GlideApp.with(context)
                .as(byte[].class)
                .load(writeVideoToFile().getAbsolutePath())
                .frame(TimeUnit.SECONDS.toMicros(1))
                .submit());

    assertThat(data).isNotNull();
    assertThat(BitmapFactory.decodeByteArray(data, 0, data.length)).isNotNull();
  }

  @Test
  public void loadVideoFileUri_asBytes_providesByteOfFrame() throws IOException {
    byte[] data =
        concurrency.get(
            Glide.with(context).as(byte[].class).load(Uri.fromFile(writeVideoToFile())).submit());

    assertThat(data).isNotNull();
    assertThat(BitmapFactory.decodeByteArray(data, 0, data.length)).isNotNull();
  }

  @Test
  public void loadVideoFileUri_asBytes_withFrameTime_providesByteOfFrame() throws IOException {
    byte[] data =
        concurrency.get(
            GlideApp.with(context)
                .as(byte[].class)
                .load(Uri.fromFile(writeVideoToFile()))
                .frame(TimeUnit.SECONDS.toMicros(1))
                .submit());

    assertThat(data).isNotNull();
    assertThat(BitmapFactory.decodeByteArray(data, 0, data.length)).isNotNull();
  }

  private File writeVideoToFile() throws IOException {
    byte[] videoData = loadVideoBytes();
    File parent = context.getCacheDir();
    if (!parent.mkdirs() && (!parent.exists() || !parent.isDirectory())) {
      throw new IllegalStateException("Failed to mkdirs for: " + parent);
    }
    File toWrite = new File(parent, "temp.jpeg");
    if (toWrite.exists() && !toWrite.delete()) {
      throw new IllegalStateException("Failed to delete existing temp file: " + toWrite);
    }

    OutputStream os = null;
    try {
      os = new BufferedOutputStream(new FileOutputStream(toWrite));
      os.write(videoData);
      os.close();
    } finally {
      if (os != null) {
        os.close();
      }
    }
    return toWrite;
  }

  private byte[] loadVideoBytes() throws IOException {
    Resources resources = context.getResources();
    InputStream is = resources.openRawResource(ResourceIds.raw.video);
    return ByteStreams.toByteArray(is);
  }
}
