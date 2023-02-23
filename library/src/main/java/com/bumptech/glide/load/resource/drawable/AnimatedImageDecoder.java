package com.bumptech.glide.load.resource.drawable;

import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.ImageDecoder.Source;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.ImageHeaderParser.ImageType;
import com.bumptech.glide.load.ImageHeaderParserUtils;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.resource.DefaultOnHeaderDecodedListener;
import com.bumptech.glide.util.ByteBufferUtil;
import com.bumptech.glide.util.Synthetic;
import com.bumptech.glide.util.Util;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Allows decoding animated images using {@link ImageDecoder}.
 *
 * <p>Supported formats: WebP on Android P+. AVIF on Android 12/S+.
 */
@RequiresApi(Build.VERSION_CODES.P)
public final class AnimatedImageDecoder {
  private final List<ImageHeaderParser> imageHeaderParsers;
  private final ArrayPool arrayPool;

  public static ResourceDecoder<InputStream, Drawable> streamDecoder(
      List<ImageHeaderParser> imageHeaderParsers, ArrayPool arrayPool) {
    return new StreamAnimatedImageDecoder(new AnimatedImageDecoder(imageHeaderParsers, arrayPool));
  }

  public static ResourceDecoder<ByteBuffer, Drawable> byteBufferDecoder(
      List<ImageHeaderParser> imageHeaderParsers, ArrayPool arrayPool) {
    return new ByteBufferAnimatedImageDecoder(
        new AnimatedImageDecoder(imageHeaderParsers, arrayPool));
  }

  private AnimatedImageDecoder(List<ImageHeaderParser> imageHeaderParsers, ArrayPool arrayPool) {
    this.imageHeaderParsers = imageHeaderParsers;
    this.arrayPool = arrayPool;
  }

  @Synthetic
  boolean handles(ByteBuffer byteBuffer) throws IOException {
    return isHandled(ImageHeaderParserUtils.getType(imageHeaderParsers, byteBuffer));
  }

  @Synthetic
  boolean handles(InputStream is) throws IOException {
    return isHandled(ImageHeaderParserUtils.getType(imageHeaderParsers, is, arrayPool));
  }

  private boolean isHandled(ImageType imageType) {
    return imageType == ImageType.ANIMATED_WEBP
        || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && imageType == ImageType.ANIMATED_AVIF);
  }

  @Synthetic
  Resource<Drawable> decode(@NonNull Source source, int width, int height, @NonNull Options options)
      throws IOException {
    Drawable decoded =
        ImageDecoder.decodeDrawable(
            source, new DefaultOnHeaderDecodedListener(width, height, options));
    if (!(decoded instanceof AnimatedImageDrawable)) {
      throw new IOException(
          "Received unexpected drawable type for animated image, failing: " + decoded);
    }
    return new AnimatedImageDrawableResource((AnimatedImageDrawable) decoded);
  }

  private static final class AnimatedImageDrawableResource implements Resource<Drawable> {
    /** A totally made up number of the number of frames we think are held in memory at once... */
    private static final int ESTIMATED_NUMBER_OF_FRAMES = 2;

    private final AnimatedImageDrawable imageDrawable;

    AnimatedImageDrawableResource(AnimatedImageDrawable imageDrawable) {
      this.imageDrawable = imageDrawable;
    }

    @NonNull
    @Override
    public Class<Drawable> getResourceClass() {
      return Drawable.class;
    }

    @NonNull
    @Override
    public AnimatedImageDrawable get() {
      return imageDrawable;
    }

    @Override
    public int getSize() {
      return imageDrawable.getIntrinsicWidth()
          * imageDrawable.getIntrinsicHeight()
          * Util.getBytesPerPixel(Bitmap.Config.ARGB_8888)
          * ESTIMATED_NUMBER_OF_FRAMES;
    }

    @Override
    public void recycle() {
      imageDrawable.stop();
      imageDrawable.clearAnimationCallbacks();
    }
  }

  private static final class StreamAnimatedImageDecoder
      implements ResourceDecoder<InputStream, Drawable> {

    private final AnimatedImageDecoder delegate;

    StreamAnimatedImageDecoder(AnimatedImageDecoder delegate) {
      this.delegate = delegate;
    }

    @Override
    public boolean handles(@NonNull InputStream source, @NonNull Options options)
        throws IOException {
      return delegate.handles(source);
    }

    @Override
    public Resource<Drawable> decode(
        @NonNull InputStream is, int width, int height, @NonNull Options options)
        throws IOException {
      Source source = ImageDecoder.createSource(ByteBufferUtil.fromStream(is));
      return delegate.decode(source, width, height, options);
    }
  }

  private static final class ByteBufferAnimatedImageDecoder
      implements ResourceDecoder<ByteBuffer, Drawable> {

    private final AnimatedImageDecoder delegate;

    ByteBufferAnimatedImageDecoder(AnimatedImageDecoder delegate) {
      this.delegate = delegate;
    }

    @Override
    public boolean handles(@NonNull ByteBuffer source, @NonNull Options options)
        throws IOException {
      return delegate.handles(source);
    }

    @Override
    public Resource<Drawable> decode(
        @NonNull ByteBuffer byteBuffer, int width, int height, @NonNull Options options)
        throws IOException {
      Source source = ImageDecoder.createSource(byteBuffer);
      return delegate.decode(source, width, height, options);
    }
  }
}
