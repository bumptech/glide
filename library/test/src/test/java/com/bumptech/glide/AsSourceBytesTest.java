package com.bumptech.glide;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bytes.SourceBytes;
import com.bumptech.glide.tests.TearDownGlide;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/** Loads {@link SourceBytes} through Glide's default registry. */
@RunWith(AndroidJUnit4.class)
// Robolectric's native graphics, which encode and decode real JPEGs, need API 26 or higher.
@Config(sdk = Build.VERSION_CODES.Q)
public class AsSourceBytesTest {
  private static final int JPEG_WIDTH = 64;
  private static final int JPEG_HEIGHT = 48;

  @Rule public final TearDownGlide tearDownGlide = new TearDownGlide();
  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  private final Context context = ApplicationProvider.getApplicationContext();
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  @After
  public void tearDown() {
    executor.shutdownNow();
  }

  @Test
  public void load_jpegFile_returnsJpegBytesUnchanged() throws Exception {
    byte[] jpeg = createJpeg();
    // A JPEG starts with the SOI marker FF D8.
    assertThat(Arrays.copyOf(jpeg, 2)).isEqualTo(new byte[] {(byte) 0xFF, (byte) 0xD8});
    File file = writeToFile(jpeg);

    SourceBytes result =
        load(() -> Glide.with(context).as(SourceBytes.class).load(file).submit().get());

    assertThat(result.getBytes()).isEqualTo(jpeg);
    Bitmap decoded =
        BitmapFactory.decodeByteArray(result.getBytes(), /* offset= */ 0, result.getBytes().length);
    assertThat(decoded.getWidth()).isEqualTo(JPEG_WIDTH);
    assertThat(decoded.getHeight()).isEqualTo(JPEG_HEIGHT);
  }

  @Test
  public void load_jpegFileAsByteArray_returnsReencodedBytes() throws Exception {
    // as(byte[].class) decodes the JPEG and compresses it again, which SourceBytes avoids.
    byte[] jpeg = createJpeg();
    File file = writeToFile(jpeg);

    byte[] result = load(() -> Glide.with(context).as(byte[].class).load(file).submit().get());

    assertThat(result).isNotEqualTo(jpeg);
  }

  @Test
  public void load_file_returnsFileBytesUnchanged() throws Exception {
    byte[] data = randomBytes();
    File file = writeToFile(data);

    SourceBytes result =
        load(() -> Glide.with(context).as(SourceBytes.class).load(file).submit().get());

    assertThat(result.getBytes()).isEqualTo(data);
  }

  @Test
  public void load_byteArray_returnsBytesUnchanged() throws Exception {
    byte[] data = randomBytes();

    SourceBytes result =
        load(() -> Glide.with(context).as(SourceBytes.class).load(data).submit().get());

    assertThat(result.getBytes()).isEqualTo(data);
  }

  @Test
  public void load_withDiskCacheStrategyAll_returnsBytesUnchanged() throws Exception {
    // DiskCacheStrategy.ALL caches the loaded resource, which fails without an encoder for it.
    byte[] data = randomBytes();
    File file = writeToFile(data);

    SourceBytes result =
        load(
            () ->
                Glide.with(context)
                    .as(SourceBytes.class)
                    .load(file)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .submit()
                    .get());

    assertThat(result.getBytes()).isEqualTo(data);
  }

  /** Runs a blocking load off the main thread, which Glide requires for {@code get()}. */
  private <T> T load(Callable<T> load) throws Exception {
    return executor.submit(load).get(5, TimeUnit.SECONDS);
  }

  /** Returns a JPEG of random pixels, encoded at quality 80. */
  private static byte[] createJpeg() {
    Bitmap bitmap = Bitmap.createBitmap(JPEG_WIDTH, JPEG_HEIGHT, Bitmap.Config.ARGB_8888);
    Random random = new Random(/* seed= */ 1);
    for (int y = 0; y < JPEG_HEIGHT; y++) {
      for (int x = 0; x < JPEG_WIDTH; x++) {
        bitmap.setPixel(
            x, y, Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
      }
    }
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.JPEG, /* quality= */ 80, os);
    return os.toByteArray();
  }

  private static byte[] randomBytes() {
    // Larger than the buffers used to read data.
    byte[] data = new byte[100_000];
    new Random(/* seed= */ 1).nextBytes(data);
    return data;
  }

  private File writeToFile(byte[] data) throws IOException {
    File file = temporaryFolder.newFile();
    try (OutputStream os = new FileOutputStream(file)) {
      os.write(data);
    }
    return file;
  }
}
