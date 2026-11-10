package com.bumptech.glide.load.resource.bitmap;

import static com.google.common.truth.Truth.assertThat;

import android.os.Build;
import android.os.ParcelFileDescriptor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.load.ImageHeaderParser.ImageType;
import com.bumptech.glide.load.Options;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowBuild;

@RunWith(AndroidJUnit4.class)
@Config(sdk = Build.VERSION_CODES.Q)
public final class ImageDecoderSupportTest {

  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  private Options options;

  @Before
  public void setUp() {
    options = new Options();
  }

  @Test
  public void hasExplicitVideoFrame_withoutTargetFrame_returnsFalse() {
    assertThat(ImageDecoderSupport.hasExplicitVideoFrame(options)).isFalse();
  }

  @Test
  public void hasExplicitVideoFrame_withDefaultFrame_returnsFalse() {
    options.set(VideoDecoder.TARGET_FRAME, VideoDecoder.DEFAULT_FRAME);
    assertThat(ImageDecoderSupport.hasExplicitVideoFrame(options)).isFalse();
  }

  @Test
  public void hasExplicitVideoFrame_withZeroFrame_returnsFalse() {
    options.set(VideoDecoder.TARGET_FRAME, 0L);
    assertThat(ImageDecoderSupport.hasExplicitVideoFrame(options)).isFalse();
  }

  @Test
  public void hasExplicitVideoFrame_withPositiveFrame_returnsTrue() {
    options.set(VideoDecoder.TARGET_FRAME, 1000L);
    assertThat(ImageDecoderSupport.hasExplicitVideoFrame(options)).isTrue();
  }

  @Test
  public void isSupportedImageType_withUnknown_returnsFalse() {
    assertThat(ImageDecoderSupport.isSupportedImageType(ImageType.UNKNOWN)).isFalse();
  }

  @Test
  public void isSupportedImageType_withJpeg_returnsTrue() {
    assertThat(ImageDecoderSupport.isSupportedImageType(ImageType.JPEG)).isTrue();
  }

  @Test
  public void isSafeToTryDecoding_onNonHuaweiOrHonorDeviceWithLargeFile_returnsTrue()
      throws IOException {
    ShadowBuild.setManufacturer("Google");
    File file = createFileOfSize(512L * 1024 * 1024 + 1);
    try (ParcelFileDescriptor pfd =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)) {
      assertThat(ImageDecoderSupport.isSafeToTryDecoding(pfd)).isTrue();
    }
  }

  @Test
  public void isSafeToTryDecoding_onHonorDeviceWithinLimit_returnsTrue() throws IOException {
    ShadowBuild.setManufacturer("HONOR");
    File file = createFileOfSize(512L * 1024 * 1024);
    try (ParcelFileDescriptor pfd =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)) {
      assertThat(ImageDecoderSupport.isSafeToTryDecoding(pfd)).isTrue();
    }
  }

  @Test
  public void isSafeToTryDecoding_onHonorDeviceAboveLimit_returnsFalse() throws IOException {
    ShadowBuild.setManufacturer("HONOR");
    File file = createFileOfSize(512L * 1024 * 1024 + 1);
    try (ParcelFileDescriptor pfd =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)) {
      assertThat(ImageDecoderSupport.isSafeToTryDecoding(pfd)).isFalse();
    }
  }

  @Test
  public void isSafeToTryDecoding_onHuaweiDeviceAboveLimit_returnsFalse() throws IOException {
    ShadowBuild.setManufacturer("HUAWEI");
    File file = createFileOfSize(512L * 1024 * 1024 + 1);
    try (ParcelFileDescriptor pfd =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)) {
      assertThat(ImageDecoderSupport.isSafeToTryDecoding(pfd)).isFalse();
    }
  }

  private File createFileOfSize(long sizeBytes) throws IOException {
    File file = temporaryFolder.newFile();
    try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
      raf.setLength(sizeBytes);
    }
    return file;
  }
}
