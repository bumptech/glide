package com.bumptech.glide.request;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
@SuppressWarnings({"PMD.UseUtilityClass", "unused"})
public final class RequestOptions extends BaseRequestOptions<RequestOptions> {

  /**
   * Returns a {@link RequestOptions} object with {@link #sizeMultiplier(float)} set.
   */
  public static RequestOptions sizeMultiplierOf(float sizeMultiplier) {
    return new RequestOptions().sizeMultiplier(sizeMultiplier);
  }

  /**
   * Returns a {@link RequestOptions} object with {@link #sizeMultiplier(float)} set.
   */
  public static RequestOptions diskCacheStrategyOf(@NonNull DiskCacheStrategy diskCacheStrategy) {
    return new RequestOptions().diskCacheStrategy(diskCacheStrategy);
  }

  /**
   * Returns a {@link RequestOptions} object with {@link #priority(Priority)}} set.
   */
  public static RequestOptions priorityOf(@NonNull Priority priority) {
    return new RequestOptions().priority(priority);
  }

  /**
   * Returns a {@link RequestOptions} object with {@link #placeholder(Drawable)} set.
   */
  public static RequestOptions placeholderOf(@Nullable Drawable placeholder) {
    return new RequestOptions().placeholder(placeholder);
  }

  /**
   * Returns a {@link RequestOptions} object with {@link #placeholder(int)} set.
   */
  public static RequestOptions placeholderOf(int placeholderId) {
    return new RequestOptions().placeholder(placeholderId);
  }

  /**
   * Returns a {@link RequestOptions} object with {@link #error(Drawable)} set.
   */
  public static RequestOptions errorOf(@Nullable Drawable errorDrawable) {
    return new RequestOptions().error(errorDrawable);
  }

  /**
   * Returns a {@link RequestOptions} object with {@link #error(int)}} set.
   */
  public static RequestOptions errorOf(int errorId) {
    return new RequestOptions().error(errorId);
  }

  /**
   * Returns a {@link RequestOptions} object with {@link #skipMemoryCache(boolean)} set.
   */
  public static RequestOptions skipMemoryCacheOf(boolean skipMemoryCache) {
    return new RequestOptions().skipMemoryCache(skipMemoryCache);
  }

  /**
   * Returns a {@link RequestOptions} object with {@link #override(int, int)}} set.
   */
  public static RequestOptions overrideOf(int width, int height) {
    return new RequestOptions().override(width, height);
  }

  /**
   * Returns a {@link RequestOptions} with {@link #override(int, int)} set where both the width and
   * height are the given size.
   */
  public static RequestOptions overrideOf(int size) {
    return overrideOf(size, size);
  }

  /**
   * Returns a {@link RequestOptions} object with {@link #signature} set.
   */
  public static RequestOptions signatureOf(@NonNull Key signature) {
    return new RequestOptions().signature(signature);
  }

  /**
   * Returns a {@link RequestOptions} object with {@link #fitCenter(Context)} set.
   */
  public static RequestOptions fitCenterTransform(Context context) {
    return new RequestOptions().fitCenter(context);
  }

  /**
   * Returns a {@link RequestOptions} object with {@link #circleCrop(Context)} set.
   */
  public static RequestOptions centerCropTransform(Context context) {
    return new RequestOptions().centerCrop(context);
  }

  /**
   * Returns a {@link RequestOptions} object with {@link RequestOptions#circleCrop(Context)} set.
   */
  public static RequestOptions circleCropTransform(Context context) {
    return new RequestOptions().circleCrop(context);
  }

  /**
   * Returns a {@link RequestOptions} object with {@link #transform(Context, Transformation)} set.
   */
  public static RequestOptions bitmapTransform(Context context,
      @NonNull Transformation<Bitmap> transformation) {
    return new RequestOptions().transform(context, transformation);
  }

  /**
   * Returns a {@link RequestOptions} object with {@link #dontTransform()} set.
   */
  public static RequestOptions noTransform() {
    return new RequestOptions().dontTransform();
  }

  /**
   * Returns a {@link RequestOptions} object with the given {@link Option} set via
   * {@link #set(Option, Object)}.
   */
  public static <T> RequestOptions option(@NonNull Option<T> option, @NonNull T value) {
    return new RequestOptions().set(option, value);
  }

  /**
   * Returns a {@link RequestOptions} object with {@link #decode(Class)} set.
   */
  public static RequestOptions decodeTypeOf(@NonNull Class<?> resourceClass) {
    return new RequestOptions().decode(resourceClass);
  }

  /**
   * Returns a {@link RequestOptions} object with {@link #format(DecodeFormat)} set.
   */
  public static RequestOptions formatOf(@NonNull DecodeFormat format) {
    return new RequestOptions().format(format);
  }

  /**
   * Returns a {@link RequestOptions} object with {@link #frame(long)} set.
   */
  public static RequestOptions frameOf(long frameTimeMicros) {
    return new RequestOptions().frame(frameTimeMicros);
  }

  /**
   * Returns a {@link RequestOptions} object with {@link #downsample(DownsampleStrategy)} set.
   */
  public static RequestOptions downsampleOf(@NonNull DownsampleStrategy strategy) {
    return new RequestOptions().downsample(strategy);
  }

  /**
   * Returns a {@link com.bumptech.glide.request.RequestOptions} with {@link
   * #encodeQuality(int)} called with the given quality.
   */
  public static RequestOptions encodeQualityOf(int quality) {
    return new RequestOptions().encodeQuality(quality);
  }

  /**
   * Returns a {@link com.bumptech.glide.request.RequestOptions} with {@link
   * #encodeFormat(android.graphics.Bitmap.CompressFormat)} called with the given format.
   */
  public static RequestOptions encodeFormatOf(@NonNull Bitmap.CompressFormat format) {
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
