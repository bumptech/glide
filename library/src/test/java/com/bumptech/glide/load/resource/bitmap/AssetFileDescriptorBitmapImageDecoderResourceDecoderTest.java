package com.bumptech.glide.load.resource.bitmap;

import static com.google.common.truth.Truth.assertThat;

import android.content.res.AssetFileDescriptor;
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
import com.google.common.io.ByteStreams;
import java.io.File;
import java.io.FileInputStream;
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
public final class AssetFileDescriptorBitmapImageDecoderResourceDecoderTest {

  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  private AssetFileDescriptorBitmapImageDecoderResourceDecoder decoder;
  private Options options;

  @Before
  public void setUp() {
    ImmutableList<ImageHeaderParser> parsers = ImmutableList.of(new DefaultImageHeaderParser());
    decoder =
        new AssetFileDescriptorBitmapImageDecoderResourceDecoder(
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
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        AssetFileDescriptor afd =
            new AssetFileDescriptor(
                pfd, /* startOffset= */ 0, AssetFileDescriptor.UNKNOWN_LENGTH)) {
      options.set(VideoDecoder.TARGET_FRAME, 0L);
      assertThat(decoder.handles(afd, options)).isFalse();
    }
  }

  @Test
  public void handles_withGifImage_returnsTrue() throws IOException {
    File file = temporaryFolder.newFile("test.gif");
    try (FileOutputStream fos = new FileOutputStream(file)) {
      fos.write(new byte[] {'G', 'I', 'F', '8', '9', 'a', 0, 0, 0, 0});
    }
    try (ParcelFileDescriptor pfd =
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        AssetFileDescriptor afd =
            new AssetFileDescriptor(
                pfd, /* startOffset= */ 0, AssetFileDescriptor.UNKNOWN_LENGTH)) {
      assertThat(decoder.handles(afd, options)).isTrue();
    }
  }

  @Test
  public void handles_withPngAssetFileDescriptor_returnsTrue() throws IOException {
    File file = createPngFile();
    try (ParcelFileDescriptor pfd =
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        AssetFileDescriptor afd =
            new AssetFileDescriptor(
                pfd, /* startOffset= */ 0, AssetFileDescriptor.UNKNOWN_LENGTH)) {
      assertThat(decoder.handles(afd, options)).isTrue();
    }
  }

  @Test
  public void handles_withZeroTargetFrame_returnsTrue() throws IOException {
    File file = createPngFile();
    try (ParcelFileDescriptor pfd =
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        AssetFileDescriptor afd =
            new AssetFileDescriptor(
                pfd, /* startOffset= */ 0, AssetFileDescriptor.UNKNOWN_LENGTH)) {
      options.set(VideoDecoder.TARGET_FRAME, 0L);
      assertThat(decoder.handles(afd, options)).isTrue();
    }
  }

  @Test
  public void handles_withPositiveTargetFrame_returnsFalse() throws IOException {
    File file = createPngFile();
    try (ParcelFileDescriptor pfd =
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        AssetFileDescriptor afd =
            new AssetFileDescriptor(
                pfd, /* startOffset= */ 0, AssetFileDescriptor.UNKNOWN_LENGTH)) {
      options.set(VideoDecoder.TARGET_FRAME, 1000L);
      assertThat(decoder.handles(afd, options)).isFalse();
    }
  }

  @Test
  public void handles_withCorruptAssetFileDescriptor_returnsFalse() throws IOException {
    File file = temporaryFolder.newFile("corrupt.bin");
    try (FileOutputStream fos = new FileOutputStream(file)) {
      fos.write(new byte[] {0, 1, 2, 3, 4, 5});
    }
    try (ParcelFileDescriptor pfd =
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        AssetFileDescriptor afd =
            new AssetFileDescriptor(
                pfd, /* startOffset= */ 0, AssetFileDescriptor.UNKNOWN_LENGTH)) {
      assertThat(decoder.handles(afd, options)).isFalse();
    }
  }

  @Test
  public void decode_withPngAssetFileDescriptor_decodesBitmap() throws IOException {
    File file = createPngFile();
    try (ParcelFileDescriptor pfd =
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        AssetFileDescriptor afd =
            new AssetFileDescriptor(
                pfd, /* startOffset= */ 0, AssetFileDescriptor.UNKNOWN_LENGTH)) {
      Resource<Bitmap> resource = decoder.decode(afd, /* width= */ 100, /* height= */ 100, options);
      assertThat(resource).isNotNull();
      assertThat(resource.get().getPixel(0, 0)).isEqualTo(Color.RED);
    }
  }

  @Test
  public void decode_withExifRotatedJpegFile_returnsRotatedBitmap() throws IOException {
    File file = createJpegFileWithOrientation(ExifInterface.ORIENTATION_ROTATE_90);
    try (ParcelFileDescriptor pfd =
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        AssetFileDescriptor afd =
            new AssetFileDescriptor(
                pfd, /* startOffset= */ 0, AssetFileDescriptor.UNKNOWN_LENGTH)) {
      Resource<Bitmap> resource =
          decoder.decode(afd, Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL, options);
      assertThat(resource).isNotNull();
      assertThat(resource.get().getWidth()).isEqualTo(50);
      assertThat(resource.get().getHeight()).isEqualTo(100);
    }
  }

  @Test
  public void handles_withJpegAtNonZeroStartOffset_returnsTrue() throws IOException {
    File jpeg = createJpegFileWithOrientation(ExifInterface.ORIENTATION_ROTATE_90);
    int prefixLength = 1024;
    File file = prependBytes(jpeg, prefixLength);
    try (ParcelFileDescriptor pfd =
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        AssetFileDescriptor afd = new AssetFileDescriptor(pfd, prefixLength, jpeg.length())) {
      assertThat(decoder.handles(afd, options)).isTrue();
    }
  }

  /** Writes a 100x50 JPEG to disk and tags it with {@code orientation}. */
  private File createJpegFileWithOrientation(int orientation) throws IOException {
    Bitmap bmp = Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888);
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

  /** Returns a new file containing {@code prefixLength} zero bytes followed by {@code source}. */
  private File prependBytes(File source, int prefixLength) throws IOException {
    File file = temporaryFolder.newFile("prefixed_" + source.getName());
    try (FileOutputStream fos = new FileOutputStream(file);
        FileInputStream fis = new FileInputStream(source)) {
      fos.write(new byte[prefixLength]);
      ByteStreams.copy(fis, fos);
    }
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
