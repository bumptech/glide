package com.bumptech.glide.load.resource.bitmap;

import static com.google.common.truth.Truth.assertThat;

import androidx.exifinterface.media.ExifInterface;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.ExifOrientationStream;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.LruArrayPool;
import com.google.common.collect.ImmutableList;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class ExifOrientationOptionsApplierTest {

  private ImmutableList<ImageHeaderParser> parsers;
  private ArrayPool arrayPool;
  private Options options;

  @Before
  public void setUp() {
    parsers = ImmutableList.of(new DefaultImageHeaderParser());
    arrayPool = new LruArrayPool(ArrayPool.STANDARD_BUFFER_SIZE_BYTES);
    options = new Options();
  }

  @Test
  public void apply_rotatedImage_returnsCopyWithExifOrientationRequired() throws IOException {
    ExifOrientationOptionsApplier applier = new ExifOrientationOptionsApplier(parsers, arrayPool);
    ByteBuffer buffer = jpegWithOrientation(ExifInterface.ORIENTATION_ROTATE_90);

    Options result = applier.apply(buffer, options);

    assertThat(result).isNotSameInstanceAs(options);
    assertThat(result.get(Downsampler.IS_EXIF_ORIENTATION_REQUIRED)).isTrue();
    assertThat(options.get(Downsampler.IS_EXIF_ORIENTATION_REQUIRED)).isFalse();
  }

  @Test
  public void apply_rotatedImage_preservesExistingOptions() throws IOException {
    options.set(Downsampler.ALLOW_HARDWARE_CONFIG, true);
    ExifOrientationOptionsApplier applier = new ExifOrientationOptionsApplier(parsers, arrayPool);

    Options result =
        applier.apply(jpegWithOrientation(ExifInterface.ORIENTATION_ROTATE_180), options);

    assertThat(result.get(Downsampler.ALLOW_HARDWARE_CONFIG)).isTrue();
    assertThat(result.get(Downsampler.IS_EXIF_ORIENTATION_REQUIRED)).isTrue();
  }

  @Test
  public void apply_rotatedImage_doesNotMoveBufferPosition() throws IOException {
    ExifOrientationOptionsApplier applier = new ExifOrientationOptionsApplier(parsers, arrayPool);
    ByteBuffer buffer = jpegWithOrientation(ExifInterface.ORIENTATION_ROTATE_90);

    applier.apply(buffer, options);

    assertThat(buffer.position()).isEqualTo(0);
  }

  @Test
  public void apply_normalOrientation_returnsSameOptions() throws IOException {
    ExifOrientationOptionsApplier applier = new ExifOrientationOptionsApplier(parsers, arrayPool);

    Options result = applier.apply(jpegWithOrientation(ExifInterface.ORIENTATION_NORMAL), options);

    assertThat(result).isSameInstanceAs(options);
    assertThat(result.get(Downsampler.IS_EXIF_ORIENTATION_REQUIRED)).isFalse();
  }

  @Test
  public void apply_withoutArrayPool_returnsSameOptions() throws IOException {
    ExifOrientationOptionsApplier applier =
        new ExifOrientationOptionsApplier(parsers, /* arrayPool= */ null);

    Options result =
        applier.apply(jpegWithOrientation(ExifInterface.ORIENTATION_ROTATE_90), options);

    assertThat(result).isSameInstanceAs(options);
  }

  @Test
  public void apply_withoutParsers_returnsSameOptions() throws IOException {
    List<ImageHeaderParser> noParsers = ImmutableList.of();
    ExifOrientationOptionsApplier applier = new ExifOrientationOptionsApplier(noParsers, arrayPool);

    Options result =
        applier.apply(jpegWithOrientation(ExifInterface.ORIENTATION_ROTATE_90), options);

    assertThat(result).isSameInstanceAs(options);
  }

  /** Builds a minimal JPEG (SOI, padding, EOI) with an injected EXIF orientation segment. */
  private static ByteBuffer jpegWithOrientation(int orientation) throws IOException {
    byte[] minimalJpeg = new byte[20];
    minimalJpeg[0] = (byte) 0xFF;
    minimalJpeg[1] = (byte) 0xD8;
    minimalJpeg[18] = (byte) 0xFF;
    minimalJpeg[19] = (byte) 0xD9;
    byte[] withExif =
        new ExifOrientationStream(new ByteArrayInputStream(minimalJpeg), orientation)
            .readAllBytes();
    return ByteBuffer.wrap(withExif);
  }
}
