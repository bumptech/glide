package com.bumptech.glide.request;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy;

/**
 * The standard concrete implementation of {@link com.bumptech.glide.request.BaseRequestOptions}.
 *
 * <p> Uses with custom types, transformations, or options can subclass and customize. {@link
 * com.bumptech.glide.request.BaseRequestOptions}. </p>
 */
@SuppressWarnings("PMD.UseUtilityClass")
public final class RequestOptions extends BaseRequestOptions<RequestOptions> {

  public static RequestOptions sizeMultiplierOf(float sizeMultiplier) {
    return new RequestOptions().sizeMultiplier(sizeMultiplier);
  }

  public static RequestOptions diskCacheStrategyOf(DiskCacheStrategy diskCacheStrategy) {
    return new RequestOptions().diskCacheStrategy(diskCacheStrategy);
  }

  public static RequestOptions priorityOf(Priority priority) {
    return new RequestOptions().priority(priority);
  }

  public static RequestOptions placeholderOf(Drawable placeholder) {
    return new RequestOptions().placeholder(placeholder);
  }

  public static RequestOptions placeholderOf(int placeholderId) {
    return new RequestOptions().placeholder(placeholderId);
  }

  public static RequestOptions errorOf(Drawable errorDrawable) {
    return new RequestOptions().error(errorDrawable);
  }

  public static RequestOptions errorOf(int errorId) {
    return new RequestOptions().error(errorId);
  }

  public static RequestOptions skipMemoryCacheOf(boolean skipMemoryCache) {
    return new RequestOptions().skipMemoryCache(skipMemoryCache);
  }

  public static RequestOptions overrideOf(int width, int height) {
    return new RequestOptions().override(width, height);
  }

  public static RequestOptions signatureOf(Key signature) {
    return new RequestOptions().signature(signature);
  }

  public static RequestOptions tagOf(String tag) {
    return new RequestOptions().tag(tag);
  }

  public static RequestOptions fitCenterTransform(Context context) {
    return new RequestOptions().fitCenter(context);
  }

  public static RequestOptions centerCropTransform(Context context) {
    return new RequestOptions().centerCrop(context);
  }

  public static RequestOptions bitmapTransform(Context context,
      Transformation<Bitmap> transformation) {
    return new RequestOptions().transform(context, transformation);
  }

  public static RequestOptions noTransform() {
    return new RequestOptions().dontTransform();
  }

  public static <T> RequestOptions option(Option<T> option, T value) {
    return new RequestOptions().set(option, value);
  }

  public static RequestOptions decodeTypeOf(Class<?> resourceClass) {
    return new RequestOptions().decode(resourceClass);
  }

  public static RequestOptions formatOf(DecodeFormat format) {
    return new RequestOptions().format(format);
  }

  public static RequestOptions frameOf(int frame) {
    return new RequestOptions().frame(frame);
  }

  public static RequestOptions downsampleOf(DownsampleStrategy strategy) {
    return new RequestOptions().downsample(strategy);
  }

  /**
   * Returns a new {@link com.bumptech.glide.request.RequestOptions} with {@link
   * #encodeQuality(Integer)} called with the given quality.
   */
  public static RequestOptions encodeQualityOf(Integer quality) {
    return new RequestOptions().encodeQuality(quality);
  }

  /**
   * Returns a new {@link com.bumptech.glide.request.RequestOptions} with {@link
   * #encodeFormat(android.graphics.Bitmap.CompressFormat)} called with the given format.
   */
  public static RequestOptions encodeFormatOf(Bitmap.CompressFormat format) {
    return new RequestOptions().encodeFormat(format);
  }

  /**
   * Returns a new {@link com.bumptech.glide.request.RequestOptions} with {@link #dontAnimate()}
   * called.
   */
  public static RequestOptions noAnimation() {
    return new RequestOptions().dontAnimate();
  }
}
