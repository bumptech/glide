package com.bumptech.glide.integration.gifdecoder;

import static com.bumptech.glide.request.RequestOptions.decodeTypeOf;

import android.graphics.Bitmap;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.annotation.GlideExtension;
import com.bumptech.glide.annotation.GlideOption;
import com.bumptech.glide.annotation.GlideType;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

/**
 * Provides API extensions for Glide that allow applications that include the gifdecoder library
 * to load GIFs and easily provide GIF specific options.
 */
@GlideExtension
public final class GifOptions {
  private static final RequestOptions DECODE_TYPE_GIF = decodeTypeOf(GifDrawable.class).lock();

  private GifOptions() { }

  /**
   * Disables resource decoders that return animated resources so any resource returned will be
   * static.
   *
   * <p> To disable transitions (cross fades, fade ins, etc) use
   * {@link com.bumptech.glide.TransitionOptions#dontTransition()}</p>
   */
  @GlideOption(
      staticMethodName = "noAnimation",
      memoizeStaticMethod = true
  )
  public static void dontAnimate(RequestOptions requestOptions) {
    requestOptions.set(ByteBufferGifDecoder.DISABLE_ANIMATION, true);
    requestOptions.set(StreamGifDecoder.DISABLE_ANIMATION, true);
  }

  /**
   * Ensures that any Bitmap transformations added to a request will apply to GIFs.
   */
  @GlideOption(override = GlideOption.OVERRIDE_EXTEND)
  public static void optionalTransform(
      RequestOptions requestOptions, Transformation<Bitmap> transformation) {
    requestOptions.optionalTransform(
        GifDrawable.class, new GifDrawableTransformation(transformation));
  }

  /**
   * Returns a new {@link RequestBuilder} that will load the resource as a {@link GifDrawable} or
   * fail.
   *
   * <p>If the underlying data is not a GIF, this will fail. As a result, this should only be used
   * if the model represents an animated GIF and the caller wants to interact with the GifDrawable
   * directly. Normally using just {@link com.bumptech.glide.RequestManager#asDrawable()} is
   * sufficient because it will determine whether or not the given data represents an animated GIF
   * and return the appropriate {@link android.graphics.drawable.Drawable}, animated or not,
   * automatically.
   */
  @GlideType(GifDrawable.class)
  public static void asGif(RequestBuilder<GifDrawable> requestBuilder) {
    requestBuilder
        .transition(new DrawableTransitionOptions())
        .apply(DECODE_TYPE_GIF);
  }
}
