package com.bumptech.glide.load.resource.bitmap;

import static com.google.common.truth.Truth.assertThat;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import androidx.exifinterface.media.ExifInterface;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.LruArrayPool;
import com.bumptech.glide.request.target.Target;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
@Config(sdk = Build.VERSION_CODES.Q)
public final class ParcelFileDescriptorBitmapImageDecoderResourceDecoderTest {

  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  private ParcelFileDescriptorBitmapImageDecoderResourceDecoder decoder;
  private Options options;

  @Before
  public void setUp() {
    ImmutableList<ImageHeaderParser> parsers = ImmutableList.of(new DefaultImageHeaderParser());
    decoder =
        new ParcelFileDescriptorBitmapImageDecoderResourceDecoder(
            parsers, new LruArrayPool(1024 * 1024));
    options = new Options();
  }

  @Test
  public void handles_withMp4VideoHeader_returnsFalse() throws IOException {
    File file = temporaryFolder.newFile("test.mp4");
    try (FileOutputStream fos = new FileOutputStream(file)) {
      fos.write(
          new byte[] {
            0, 0, 0, 24, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm', 0, 0, 2, 0, 'i', 's', 'o', 'm',
            'm', 'p', '4', '2'
          });
    }
    try (ParcelFileDescriptor pfd =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)) {
      options.set(VideoDecoder.TARGET_FRAME, 0L);
      assertThat(decoder.handles(pfd, options)).isFalse();
    }
  }

  @Test
  public void handles_withGifImage_returnsTrue() throws IOException {
    File file = temporaryFolder.newFile("test.gif");
    try (FileOutputStream fos = new FileOutputStream(file)) {
      fos.write(new byte[] {'G', 'I', 'F', '8', '9', 'a', 0, 0, 0, 0});
    }
    try (ParcelFileDescriptor pfd =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)) {
      assertThat(decoder.handles(pfd, options)).isTrue();
    }
  }

  @Test
  public void handles_withPngFileDescriptor_returnsTrue() throws IOException {
    File file = createPngFile();
    try (ParcelFileDescriptor pfd =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)) {
      assertThat(decoder.handles(pfd, options)).isTrue();
    }
  }

  @Test
  public void handles_withZeroTargetFrame_returnsTrue() throws IOException {
    File file = createPngFile();
    try (ParcelFileDescriptor pfd =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)) {
      options.set(VideoDecoder.TARGET_FRAME, 0L);
      assertThat(decoder.handles(pfd, options)).isTrue();
    }
  }

  @Test
  public void handles_withPositiveTargetFrame_returnsFalse() throws IOException {
    File file = createPngFile();
    try (ParcelFileDescriptor pfd =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)) {
      options.set(VideoDecoder.TARGET_FRAME, 1000L);
      assertThat(decoder.handles(pfd, options)).isFalse();
    }
  }

  @Test
  public void handles_withCorruptFileDescriptor_returnsFalse() throws IOException {
    File file = temporaryFolder.newFile("corrupt.bin");
    try (FileOutputStream fos = new FileOutputStream(file)) {
      fos.write(new byte[] {0, 1, 2, 3, 4, 5});
    }
    try (ParcelFileDescriptor pfd =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)) {
      assertThat(decoder.handles(pfd, options)).isFalse();
    }
  }

  @Test
  public void decode_withPngFileDescriptor_decodesBitmap() throws IOException {
    File file = createPngFile();
    try (ParcelFileDescriptor pfd =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)) {
      Resource<Bitmap> resource = decoder.decode(pfd, /* width= */ 100, /* height= */ 100, options);
      assertThat(resource).isNotNull();
      assertThat(resource.get().getPixel(0, 0)).isEqualTo(Color.RED);
    }
  }

  @Test
  public void decode_withExifRotatedJpegFile_returnsRotatedBitmap() throws IOException {
    File file =
        createJpegFileWithOrientation(
            /* width= */ 100, /* height= */ 50, ExifInterface.ORIENTATION_ROTATE_90);
    try (ParcelFileDescriptor pfd =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)) {
      Resource<Bitmap> resource =
          decoder.decode(pfd, Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL, options);
      assertThat(resource).isNotNull();
      assertThat(resource.get().getWidth()).isEqualTo(50);
      assertThat(resource.get().getHeight()).isEqualTo(100);
    }
  }

  @Test
  public void decode_withExifNormalJpegFile_returnsUnrotatedBitmap() throws IOException {
    File file =
        createJpegFileWithOrientation(
            /* width= */ 100, /* height= */ 50, ExifInterface.ORIENTATION_NORMAL);
    try (ParcelFileDescriptor pfd =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)) {
      Resource<Bitmap> resource =
          decoder.decode(pfd, Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL, options);
      assertThat(resource).isNotNull();
      assertThat(resource.get().getWidth()).isEqualTo(100);
      assertThat(resource.get().getHeight()).isEqualTo(50);
    }
  }

  /** Writes a {@code width}x{@code height} JPEG to disk and tags it with {@code orientation}. */
  private File createJpegFileWithOrientation(int width, int height, int orientation)
      throws IOException {
    Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    new Canvas(bmp).drawColor(Color.RED);

    File file = temporaryFolder.newFile("exif_" + orientation + ".jpg");
    try (FileOutputStream fos = new FileOutputStream(file)) {
      bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
    }
    ExifInterface exif = new ExifInterface(file.getAbsolutePath());
    exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(orientation));
    exif.saveAttributes();
    return file;
  }

  private File createPngFile() throws IOException {
    Bitmap bmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bmp);
    canvas.drawColor(Color.RED);

    File file = temporaryFolder.newFile("test.png");
    try (FileOutputStream fos = new FileOutputStream(file)) {
      bmp.compress(Bitmap.CompressFormat.PNG, 0, fos);
    }
    return file;
  }
}
