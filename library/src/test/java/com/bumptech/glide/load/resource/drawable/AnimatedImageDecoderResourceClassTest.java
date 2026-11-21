package com.bumptech.glide.load.resource.drawable;

import static com.google.common.truth.Truth.assertThat;

import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.LruArrayPool;
import com.bumptech.glide.load.resource.bitmap.DefaultImageHeaderParser;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

/**
 * Tests that {@link AnimatedImageDecoder} and {@link AnimatedWebpDecoder} report {@link
 * AnimatedImageDrawable} as their resource class.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public final class AnimatedImageDecoderResourceClassTest {

  private static final int IMAGE_SIZE = 1;
  private static final int ARRAY_POOL_SIZE_BYTES = 64 * 1024;

  @Test
  public void animatedImageDecoder_byteBufferDecoder_reportsAnimatedImageDrawableClass()
      throws Exception {
    List<ImageHeaderParser> parsers = Collections.singletonList(new DefaultImageHeaderParser());
    ResourceDecoder<ByteBuffer, Drawable> decoder =
        AnimatedImageDecoder.byteBufferDecoder(parsers, new LruArrayPool(ARRAY_POOL_SIZE_BYTES));
    ByteBuffer buffer = ByteBuffer.wrap(newMinimalAnimatedWebp());
    Options options = new Options();

    boolean handled = decoder.handles(buffer, options);
    Resource<Drawable> resource = decoder.decode(buffer, IMAGE_SIZE, IMAGE_SIZE, options);

    assertThat(handled).isTrue();
    assertThat(resource).isNotNull();
    try {
      assertThat(resource.getResourceClass()).isEqualTo(AnimatedImageDrawable.class);
      assertThat(resource.get()).isInstanceOf(AnimatedImageDrawable.class);
    } finally {
      resource.recycle();
    }
  }

  @Test
  public void animatedImageDecoder_streamDecoder_reportsAnimatedImageDrawableClass()
      throws Exception {
    List<ImageHeaderParser> parsers = Collections.singletonList(new DefaultImageHeaderParser());
    ArrayPool arrayPool = new LruArrayPool(ARRAY_POOL_SIZE_BYTES);
    ResourceDecoder<InputStream, Drawable> decoder =
        AnimatedImageDecoder.streamDecoder(parsers, arrayPool);
    InputStream stream = new ByteArrayInputStream(newMinimalAnimatedWebp());
    Options options = new Options();

    boolean handled = decoder.handles(stream, options);
    Resource<Drawable> resource = decoder.decode(stream, IMAGE_SIZE, IMAGE_SIZE, options);

    assertThat(handled).isTrue();
    assertThat(resource).isNotNull();
    try {
      assertThat(resource.getResourceClass()).isEqualTo(AnimatedImageDrawable.class);
      assertThat(resource.get()).isInstanceOf(AnimatedImageDrawable.class);
    } finally {
      resource.recycle();
    }
  }

  @Test
  public void animatedWebpDecoder_byteBufferDecoder_reportsAnimatedImageDrawableClass()
      throws Exception {
    List<ImageHeaderParser> parsers = Collections.singletonList(new DefaultImageHeaderParser());
    ArrayPool arrayPool = new LruArrayPool(ARRAY_POOL_SIZE_BYTES);
    ResourceDecoder<ByteBuffer, Drawable> decoder =
        newDeprecatedAnimatedWebpByteBufferDecoder(parsers, arrayPool);
    ByteBuffer buffer = ByteBuffer.wrap(newMinimalAnimatedWebp());
    Options options = new Options();

    boolean handled = decoder.handles(buffer, options);
    Resource<Drawable> resource = decoder.decode(buffer, IMAGE_SIZE, IMAGE_SIZE, options);

    assertThat(handled).isTrue();
    assertThat(resource).isNotNull();
    try {
      assertThat(resource.getResourceClass()).isEqualTo(AnimatedImageDrawable.class);
      assertThat(resource.get()).isInstanceOf(AnimatedImageDrawable.class);
    } finally {
      resource.recycle();
    }
  }

  @Test
  public void animatedWebpDecoder_streamDecoder_reportsAnimatedImageDrawableClass()
      throws Exception {
    List<ImageHeaderParser> parsers = Collections.singletonList(new DefaultImageHeaderParser());
    ArrayPool arrayPool = new LruArrayPool(ARRAY_POOL_SIZE_BYTES);
    ResourceDecoder<InputStream, Drawable> decoder =
        newDeprecatedAnimatedWebpStreamDecoder(parsers, arrayPool);
    InputStream stream = new ByteArrayInputStream(newMinimalAnimatedWebp());
    Options options = new Options();

    boolean handled = decoder.handles(stream, options);
    Resource<Drawable> resource = decoder.decode(stream, IMAGE_SIZE, IMAGE_SIZE, options);

    assertThat(handled).isTrue();
    assertThat(resource).isNotNull();
    try {
      assertThat(resource.getResourceClass()).isEqualTo(AnimatedImageDrawable.class);
      assertThat(resource.get()).isInstanceOf(AnimatedImageDrawable.class);
    } finally {
      resource.recycle();
    }
  }

  // AnimatedWebpDecoder is deprecated in favor of AnimatedImageDecoder and tested until removal.
  @SuppressWarnings("deprecation")
  private static ResourceDecoder<ByteBuffer, Drawable> newDeprecatedAnimatedWebpByteBufferDecoder(
      List<ImageHeaderParser> parsers, ArrayPool arrayPool) {
    return AnimatedWebpDecoder.byteBufferDecoder(parsers, arrayPool);
  }

  // AnimatedWebpDecoder is deprecated in favor of AnimatedImageDecoder and tested until removal.
  @SuppressWarnings("deprecation")
  private static ResourceDecoder<InputStream, Drawable> newDeprecatedAnimatedWebpStreamDecoder(
      List<ImageHeaderParser> parsers, ArrayPool arrayPool) {
    return AnimatedWebpDecoder.streamDecoder(parsers, arrayPool);
  }

  // Minimal 1x1 two-frame lossless animated WebP payload so tests run standalone without assets.
  private static byte[] newMinimalAnimatedWebp() {
    return new byte[] {
      // RIFF/WEBP header + VP8X chunk (animation flag 0x02).
      (byte) 0x52, (byte) 0x49, (byte) 0x46, (byte) 0x46,
      (byte) 0x84, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0x57, (byte) 0x45, (byte) 0x42, (byte) 0x50,
      (byte) 0x56, (byte) 0x50, (byte) 0x38, (byte) 0x58,
      (byte) 0x0A, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      // VP8X canvas bounds and ANIM chunk (background + loop count).
      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0x00, (byte) 0x00, (byte) 0x41, (byte) 0x4E,
      (byte) 0x49, (byte) 0x4D, (byte) 0x06, (byte) 0x00,
      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      // Frame 1: ANMF chunk containing a 1x1 lossless VP8L frame.
      (byte) 0x41, (byte) 0x4E, (byte) 0x4D, (byte) 0x46,
      (byte) 0x28, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0x64, (byte) 0x00, (byte) 0x00, (byte) 0x02,
      (byte) 0x56, (byte) 0x50, (byte) 0x38, (byte) 0x4C,
      // Frame 1 VP8L payload + padding byte.
      (byte) 0x0F, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0x2F, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0x00, (byte) 0x07, (byte) 0x10, (byte) 0xFD,
      (byte) 0x8F, (byte) 0xFE, (byte) 0x07, (byte) 0x22,
      (byte) 0xA2, (byte) 0xFF, (byte) 0x01, (byte) 0x00,
      // Frame 2: ANMF chunk containing a 1x1 lossless VP8L frame.
      (byte) 0x41, (byte) 0x4E, (byte) 0x4D, (byte) 0x46,
      (byte) 0x28, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0x64, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0x56, (byte) 0x50, (byte) 0x38, (byte) 0x4C,
      // Frame 2 VP8L payload + padding byte.
      (byte) 0x0F, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0x2F, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0x00, (byte) 0x07, (byte) 0x10, (byte) 0xD1,
      (byte) 0xFF, (byte) 0xFE, (byte) 0x07, (byte) 0x22,
      (byte) 0xA2, (byte) 0xFF, (byte) 0x01, (byte) 0x00,
    };
  }
}
