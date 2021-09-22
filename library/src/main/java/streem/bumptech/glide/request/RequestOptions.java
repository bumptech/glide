package com.bumptech.glide.request;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import androidx.annotation.CheckResult;
import androidx.annotation.DrawableRes;
import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy;

/**
 * Provides type independent options to customize loads with Glide.
 *
 * <p>Non-final to allow Glide's generated classes to be assignable to their non-generated
 * equivalents.
 */
@SuppressWarnings("PMD.UseUtilityClass")
public class RequestOptions extends BaseRequestOptions<RequestOptions> {

  @Nullable private static RequestOptions skipMemoryCacheTrueOptions;
  @Nullable private static RequestOptions skipMemoryCacheFalseOptions;
  @Nullable private static RequestOptions fitCenterOptions;
  @Nullable private static RequestOptions centerInsideOptions;
  @Nullable private static RequestOptions centerCropOptions;
  @Nullable private static RequestOptions circleCropOptions;
  @Nullable private static RequestOptions noTransformOptions;
  @Nullable private static RequestOptions noAnimationOptions;

  /** Returns a {@link RequestOptions} object with {@link #sizeMultiplier(float)} set. */
  @SuppressWarnings("WeakerAccess") // Public API
  @NonNull
  @CheckResult
  public static RequestOptions sizeMultiplierOf(
      @FloatRange(from = 0, to = 1) float sizeMultiplier) {
    return new RequestOptions().sizeMultiplier(sizeMultiplier);
  }

  /**
   * Returns a {@link RequestOptions} object with {@link #diskCacheStrategy(DiskCacheStrategy)} set.
   */
  @NonNull
  @CheckResult
  public static RequestOptions diskCacheStrategyOf(@NonNull DiskCacheStrategy diskCacheStrategy) {
    return new RequestOptions().diskCacheStrategy(diskCacheStrategy);
  }

  /**
   * Returns a {@link RequestOptions} object with {@link BaseRequestOptions#priority(Priority)}}
   * set.
   */
  @SuppressWarnings("WeakerAccess") // Public API
  @NonNull
  @CheckResult
  public static RequestOptions priorityOf(@NonNull Priority priority) {
    return new RequestOptions().priority(priority);
  }

  /** Returns a {@link RequestOptions} object with {@link #placeholder(Drawable)} set. */
  @NonNull
  @CheckResult
  public static RequestOptions placeholderOf(@Nullable Drawable placeholder) {
    return new RequestOptions().placeholder(placeholder);
  }

  /** Returns a {@link RequestOptions} object with {@link #placeholder(int)} set. */
  @NonNull
  @CheckResult
  public static RequestOptions placeholderOf(@DrawableRes int placeholderId) {
    return new RequestOptions().placeholder(placeholderId);
  }

  /** Returns a {@link RequestOptions} object with {@link #error(Drawable)} set. */
  @NonNull
  @CheckResult
  public static RequestOptions errorOf(@Nullable Drawable errorDrawable) {
    return new RequestOptions().error(errorDrawable);
  }

  /** Returns a {@link RequestOptions} object with {@link #error(int)}} set. */
  @NonNull
  @CheckResult
  public static RequestOptions errorOf(@DrawableRes int errorId) {
    return new RequestOptions().error(errorId);
  }

  /** Returns a {@link RequestOptions} object with {@link #skipMemoryCache(boolean)} set. */
  @NonNull
  @CheckResult
  public static RequestOptions skipMemoryCacheOf(boolean skipMemoryCache) {
    if (skipMemoryCache) {
      if (skipMemoryCacheTrueOptions == null) {
        skipMemoryCacheTrueOptions = new RequestOptions().skipMemoryCache(true).autoClone();
      }
      return skipMemoryCacheTrueOptions;
    } else {
      if (skipMemoryCacheFalseOptions == null) {
        skipMemoryCacheFalseOptions = new RequestOptions().skipMemoryCache(false).autoClone();
      }
      return skipMemoryCacheFalseOptions;
    }
  }

  /** Returns a {@link RequestOptions} object with {@link #override(int, int)}} set. */
  @SuppressWarnings("WeakerAccess") // Public API
  @NonNull
  @CheckResult
  public static RequestOptions overrideOf(int width, int height) {
    return new RequestOptions().override(width, height);
  }

  /**
   * Returns a {@link RequestOptions} with {@link #override(int, int)} set where both the width and
   * height are the given size.
   */
  @SuppressWarnings("WeakerAccess") // Public API
  @NonNull
  @CheckResult
  public static RequestOptions overrideOf(int size) {
    return overrideOf(size, size);
  }

  /** Returns a {@link RequestOptions} object with {@link #signature} set. */
  @NonNull
  @CheckResult
  public static RequestOptions signatureOf(@NonNull Key signature) {
    return new RequestOptions().signature(signature);
  }

  /** Returns a {@link RequestOptions} object with {@link #fitCenter()} set. */
  @NonNull
  @CheckResult
  public static RequestOptions fitCenterTransform() {
    if (fitCenterOptions == null) {
      fitCenterOptions = new RequestOptions().fitCenter().autoClone();
    }
    return fitCenterOptions;
  }

  /** Returns a {@link RequestOptions} object with {@link #centerInside()} set. */
  @SuppressWarnings("WeakerAccess") // Public API
  @NonNull
  @CheckResult
  public static RequestOptions centerInsideTransform() {
    if (centerInsideOptions == null) {
      centerInsideOptions = new RequestOptions().centerInside().autoClone();
    }
    return centerInsideOptions;
  }

  /** Returns a {@link RequestOptions} object with {@link #centerCrop()} set. */
  @SuppressWarnings("WeakerAccess") // Public API
  @NonNull
  @CheckResult
  public static RequestOptions centerCropTransform() {
    if (centerCropOptions == null) {
      centerCropOptions = new RequestOptions().centerCrop().autoClone();
    }
    return centerCropOptions;
  }

  /** Returns a {@link RequestOptions} object with {@link RequestOptions#circleCrop()} set. */
  @SuppressWarnings("WeakerAccess") // Public API
  @NonNull
  @CheckResult
  public static RequestOptions circleCropTransform() {
    if (circleCropOptions == null) {
      circleCropOptions = new RequestOptions().circleCrop().autoClone();
    }
    return circleCropOptions;
  }

  /** Returns a {@link RequestOptions} object with {@link #transform(Transformation)} set. */
  @SuppressWarnings("WeakerAccess") // Public API
  @NonNull
  @CheckResult
  public static RequestOptions bitmapTransform(@NonNull Transformation<Bitmap> transformation) {
    return new RequestOptions().transform(transformation);
  }

  /** Returns a {@link RequestOptions} object with {@link #dontTransform()} set. */
  @SuppressWarnings("WeakerAccess")
  @NonNull
  @CheckResult
  public static RequestOptions noTransformation() {
    if (noTransformOptions == null) {
      noTransformOptions = new RequestOptions().dontTransform().autoClone();
    }
    return noTransformOptions;
  }

  /**
   * Returns a {@link RequestOptions} object with the given {@link Option} set via {@link
   * #set(Option, Object)}.
   */
  @NonNull
  @CheckResult
  public static <T> RequestOptions option(@NonNull Option<T> option, @NonNull T value) {
    return new RequestOptions().set(option, value);
  }

  /** Returns a {@link RequestOptions} object with {@link #decode(Class)} set. */
  @NonNull
  @CheckResult
  public static RequestOptions decodeTypeOf(@NonNull Class<?> resourceClass) {
    return new RequestOptions().decode(resourceClass);
  }

  /** Returns a {@link RequestOptions} object with {@link #format(DecodeFormat)} set. */
  @SuppressWarnings("WeakerAccess") // Public API
  @NonNull
  @CheckResult
  public static RequestOptions formatOf(@NonNull DecodeFormat format) {
    return new RequestOptions().format(format);
  }

  /** Returns a {@link RequestOptions} object with {@link #frame(long)} set. */
  @SuppressWarnings("WeakerAccess") // Public API
  @NonNull
  @CheckResult
  public static RequestOptions frameOf(@IntRange(from = 0) long frameTimeMicros) {
    return new RequestOptions().frame(frameTimeMicros);
  }

  /** Returns a {@link RequestOptions} object with {@link #downsample(DownsampleStrategy)} set. */
  @SuppressWarnings("WeakerAccess") // Public API
  @NonNull
  @CheckResult
  public static RequestOptions downsampleOf(@NonNull DownsampleStrategy strategy) {
    return new RequestOptions().downsample(strategy);
  }

  /** Returns a {@link RequestOptions} object with {@link #timeout(int)} set. */
  @NonNull
  @CheckResult
  public static RequestOptions timeoutOf(@IntRange(from = 0) int timeout) {
    return new RequestOptions().timeout(timeout);
  }

  /**
   * Returns a {@link com.bumptech.glide.request.RequestOptions} with {@link #encodeQuality(int)}
   * called with the given quality.
   */
  @SuppressWarnings("WeakerAccess") // Public API
  @NonNull
  @CheckResult
  public static RequestOptions encodeQualityOf(@IntRange(from = 0, to = 100) int quality) {
    return new RequestOptions().encodeQuality(quality);
  }

  /**
   * Returns a {@link com.bumptech.glide.request.RequestOptions} with {@link
   * #encodeFormat(android.graphics.Bitmap.CompressFormat)} called with the given format.
   */
  @SuppressWarnings("WeakerAccess") // Public API
  @NonNull
  @CheckResult
  public static RequestOptions encodeFormatOf(@NonNull Bitmap.CompressFormat format) {
    return new RequestOptions().encodeFormat(format);
  }

  /**
   * Returns a new {@link com.bumptech.glide.request.RequestOptions} with {@link #dontAnimate()}
   * called.
   */
  @SuppressWarnings("WeakerAccess") // Public API
  @NonNull
  @CheckResult
  public static RequestOptions noAnimation() {
    if (noAnimationOptions == null) {
      noAnimationOptions = new RequestOptions().dontAnimate().autoClone();
    }
    return noAnimationOptions;
  }
}
