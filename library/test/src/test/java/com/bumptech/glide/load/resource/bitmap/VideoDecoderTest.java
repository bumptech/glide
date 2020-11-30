package com.bumptech.glide.load.resource.bitmap;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.tests.Util;
import com.bumptech.glide.util.Preconditions;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class VideoDecoderTest {
  @Mock private ParcelFileDescriptor resource;
  @Mock private VideoDecoder.MediaMetadataRetrieverFactory factory;
  @Mock private VideoDecoder.MediaMetadataRetrieverInitializer<ParcelFileDescriptor> initializer;
  @Mock private MediaMetadataRetriever retriever;
  @Mock private BitmapPool bitmapPool;
  private VideoDecoder<ParcelFileDescriptor> decoder;
  private Options options;
  private int initialSdkVersion;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(factory.build()).thenReturn(retriever);
    decoder = new VideoDecoder<>(bitmapPool, initializer, factory);
    options = new Options();

    initialSdkVersion = Build.VERSION.SDK_INT;
  }

  @After
  public void tearDown() {
    Util.setSdkVersionInt(initialSdkVersion);
  }

  @Test
  public void testReturnsRetrievedFrameForResource() throws IOException {
    Util.setSdkVersionInt(19);
    Bitmap expected = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    when(retriever.getFrameAtTime(VideoDecoder.DEFAULT_FRAME, VideoDecoder.DEFAULT_FRAME_OPTION))
        .thenReturn(expected);

    Resource<Bitmap> result =
        Preconditions.checkNotNull(decoder.decode(resource, 100, 100, options));

    verify(initializer).initialize(retriever, resource);
    assertEquals(expected, result.get());
  }

  @Test
  public void testReleasesMediaMetadataRetriever() {
    Util.setSdkVersionInt(19);
    assertThrows(
        RuntimeException.class,
        new ThrowingRunnable() {
          @Override
          public void run() throws IOException {
            decoder.decode(resource, 1, 2, options);
          }
        });

    verify(retriever).release();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThrowsExceptionIfCalledWithInvalidFrame() throws IOException {
    Util.setSdkVersionInt(19);
    options.set(VideoDecoder.TARGET_FRAME, -5L);
    new VideoDecoder<>(bitmapPool, initializer, factory).decode(resource, 100, 100, options);
  }

  @Test
  public void testSpecifiesThumbnailFrameIfICalledWithFrameNumber() {
    Util.setSdkVersionInt(19);
    long frame = 5;
    options.set(VideoDecoder.TARGET_FRAME, frame);
    decoder = new VideoDecoder<>(bitmapPool, initializer, factory);

    assertThrows(
        RuntimeException.class,
        new ThrowingRunnable() {
          @Override
          public void run() throws IOException {
            decoder.decode(resource, 1, 2, options);
          }
        });

    verify(retriever).getFrameAtTime(frame, VideoDecoder.DEFAULT_FRAME_OPTION);
  }

  @Test
  public void testDoesNotSpecifyThumbnailFrameIfCalledWithoutFrameNumber() {
    Util.setSdkVersionInt(19);
    decoder = new VideoDecoder<>(bitmapPool, initializer, factory);
    assertThrows(
        RuntimeException.class,
        new ThrowingRunnable() {
          @Override
          public void run() throws IOException {
            decoder.decode(resource, 100, 100, options);
          }
        });

    verify(retriever).getFrameAtTime(VideoDecoder.DEFAULT_FRAME, VideoDecoder.DEFAULT_FRAME_OPTION);
  }

  @Test
  public void getScaledFrameAtTime() throws IOException {
    // Anything other than NONE.
    options.set(DownsampleStrategy.OPTION, DownsampleStrategy.AT_LEAST);

    Bitmap expected = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    when(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH))
        .thenReturn("100");
    when(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT))
        .thenReturn("100");
    when(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION))
        .thenReturn("0");
    when(retriever.getScaledFrameAtTime(-1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 100, 100))
        .thenReturn(expected);

    assertThat(decoder.decode(resource, 100, 100, options).get()).isSameInstanceAs(expected);
  }

  @Test
  public void decodeFrame_withTargetSizeOriginal_onApi27_doesNotThrow() throws IOException {
    Bitmap expected = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    when(retriever.getFrameAtTime(-1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC))
        .thenReturn(expected);

    verify(retriever, never()).getScaledFrameAtTime(anyLong(), anyInt(), anyInt(), anyInt());
    assertThat(decoder.decode(resource, Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL, options).get())
        .isSameInstanceAs(expected);
  }

  @Test
  public void decodeFrame_withTargetSizeOriginalWidthOnly_onApi27_doesNotThrow()
      throws IOException {
    Bitmap expected = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    when(retriever.getFrameAtTime(-1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC))
        .thenReturn(expected);

    verify(retriever, never()).getScaledFrameAtTime(anyLong(), anyInt(), anyInt(), anyInt());
    assertThat(decoder.decode(resource, Target.SIZE_ORIGINAL, 100, options).get())
        .isSameInstanceAs(expected);
  }

  @Test
  public void decodeFrame_withTargetSizeOriginalHeightOnly_onApi27_doesNotThrow()
      throws IOException {
    Bitmap expected = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    when(retriever.getFrameAtTime(-1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC))
        .thenReturn(expected);

    verify(retriever, never()).getScaledFrameAtTime(anyLong(), anyInt(), anyInt(), anyInt());
    assertThat(decoder.decode(resource, 100, Target.SIZE_ORIGINAL, options).get())
        .isSameInstanceAs(expected);
  }
}
