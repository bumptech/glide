package com.bumptech.glide.request;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.stream.HttpGlideUrlLoader;
import com.bumptech.glide.load.resource.bitmap.BitmapDrawableTransformation;
import com.bumptech.glide.load.resource.bitmap.BitmapEncoder;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.CenterInside;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy;
import com.bumptech.glide.load.resource.bitmap.Downsampler;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.bitmap.VideoBitmapDecoder;
import com.bumptech.glide.load.resource.gif.ByteBufferGifDecoder;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.load.resource.gif.GifDrawableTransformation;
import com.bumptech.glide.load.resource.gif.StreamGifDecoder;
import com.bumptech.glide.signature.EmptySignature;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Util;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides type independent options to customize loads with Glide.
 */
@SuppressWarnings({"PMD.UseUtilityClass", "unused"})
public class RequestOptions implements Cloneable {
  private static final int UNSET = -1;
  private static final int SIZE_MULTIPLIER = 1 << 1;
  private static final int DISK_CACHE_STRATEGY = 1 << 2;
  private static final int PRIORITY = 1 << 3;
  private static final int ERROR_PLACEHOLDER = 1 << 4;
  private static final int ERROR_ID = 1 << 5;
  private static final int PLACEHOLDER = 1 << 6;
  private static final int PLACEHOLDER_ID = 1 << 7;
  private static final int IS_CACHEABLE = 1 << 8;
  private static final int OVERRIDE = 1 << 9;
  private static final int SIGNATURE = 1 << 10;
  private static final int TRANSFORMATION = 1 << 11;
  private static final int RESOURCE_CLASS = 1 << 12;
  private static final int FALLBACK = 1 << 13;
  private static final int FALLBACK_ID = 1 << 14;
  private static final int THEME = 1 << 15;
  private static final int TRANSFORMATION_ALLOWED = 1 << 16;
  private static final int TRANSFORMATION_REQUIRED = 1 << 17;
  private static final int USE_UNLIMITED_SOURCE_GENERATORS_POOL = 1 << 18;
  private static final int ONLY_RETRIEVE_FROM_CACHE = 1 << 19;

  @Nullable
  private static RequestOptions skipMemoryCacheTrueOptions;
  @Nullable
  private static RequestOptions skipMemoryCacheFalseOptions;
  @Nullable
  private static RequestOptions fitCenterOptions;
  @Nullable
  private static RequestOptions centerInsideOptions;
  @Nullable
  private static RequestOptions centerCropOptions;
  @Nullable
  private static RequestOptions circleCropOptions;
  @Nullable
  private static RequestOptions noTransformOptions;
  @Nullable
  private static RequestOptions noAnimationOptions;

  private int fields;
  private float sizeMultiplier = 1f;
  @NonNull
  private DiskCacheStrategy diskCacheStrategy = DiskCacheStrategy.AUTOMATIC;
  @NonNull
  private Priority priority = Priority.NORMAL;
  @Nullable
  private Drawable errorPlaceholder;
  private int errorId;
  @Nullable
  private Drawable placeholderDrawable;
  private int placeholderId;
  private boolean isCacheable = true;
  private int overrideHeight = RequestOptions.UNSET;
  private int overrideWidth = RequestOptions.UNSET;
  @NonNull
  private Key signature = EmptySignature.obtain();
  private boolean isTransformationRequired;
  private boolean isTransformationAllowed = true;
  @Nullable
  private Drawable fallbackDrawable;
  private int fallbackId;
  @NonNull
  private Options options = new Options();
  @NonNull
  private Map<Class<?>, Transformation<?>> transformations = new HashMap<>();
  @NonNull
  private Class<?> resourceClass = Object.class;
  private boolean isLocked;
  @Nullable
  private Resources.Theme theme;
  private boolean isAutoCloneEnabled;
  private boolean useUnlimitedSourceGeneratorsPool;
  private boolean onlyRetrieveFromCache;
  private boolean isScaleOnlyOrNoTransform = true;

  /**
   * Returns a {@link RequestOptions} object with {@link #sizeMultiplier(float)} set.
   */
  @SuppressWarnings("WeakerAccess") // Public API
  public static RequestOptions sizeMultiplierOf(float sizeMultiplier) {
    return new RequestOptions().sizeMultiplier(sizeMultiplier);
  }

  /**
   * Returns a {@link RequestOptions} object with {@link #diskCacheStrategy(DiskCacheStrategy)} set.
   */
  public static RequestOptions diskCacheStrategyOf(@NonNull DiskCacheStrategy diskCacheStrategy) {
    return new RequestOptions().diskCacheStrategy(diskCacheStrategy);
  }

  /**
   * Returns a {@link RequestOptions} object with {@link #priority(Priority)}} set.
   */
  @SuppressWarnings("WeakerAccess") // Public API
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

  /**
   * Returns a {@link RequestOptions} object with {@link #override(int, int)}} set.
   */
  @SuppressWarnings("WeakerAccess") // Public API
  public static RequestOptions overrideOf(int width, int height) {
    return new RequestOptions().override(width, height);
  }

  /**
   * Returns a {@link RequestOptions} with {@link #override(int, int)} set where both the width and
   * height are the given size.
   */
  @SuppressWarnings("WeakerAccess") // Public API
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
   * Returns a {@link RequestOptions} object with {@link #fitCenter()} set.
   */
  public static RequestOptions fitCenterTransform() {
    if (fitCenterOptions == null) {
      fitCenterOptions = new RequestOptions()
          .fitCenter()
          .autoClone();
    }
    return fitCenterOptions;
  }

  /**
   * Returns a {@link RequestOptions} object with {@link #centerInside()} set.
   */
  @SuppressWarnings("WeakerAccess") // Public API
  public static RequestOptions centerInsideTransform() {
    if (centerInsideOptions == null) {
      centerInsideOptions = new RequestOptions()
              .centerInside()
              .autoClone();
    }
    return centerInsideOptions;
  }

  /**
   * Returns a {@link RequestOptions} object with {@link #centerCrop()} set.
   */
  @SuppressWarnings("WeakerAccess") // Public API
  public static RequestOptions centerCropTransform() {
    if (centerCropOptions == null) {
      centerCropOptions = new RequestOptions()
          .centerCrop()
          .autoClone();
    }
    return centerCropOptions;
  }

  /**
   * Returns a {@link RequestOptions} object with {@link RequestOptions#circleCrop()} set.
   */
  @SuppressWarnings("WeakerAccess") // Public API
  public static RequestOptions circleCropTransform() {
    if (circleCropOptions == null) {
      circleCropOptions = new RequestOptions()
          .circleCrop()
          .autoClone();
    }
    return circleCropOptions;
  }

  /**
   * Returns a {@link RequestOptions} object with {@link #transform(Transformation)} set.
   */
  @SuppressWarnings("WeakerAccess") // Public API
  public static RequestOptions bitmapTransform(@NonNull Transformation<Bitmap> transformation) {
    return new RequestOptions().transform(transformation);
  }

  /**
   * Returns a {@link RequestOptions} object with {@link #dontTransform()} set.
   */
  @SuppressWarnings("WeakerAccess")
  public static RequestOptions noTransformation() {
    if (noTransformOptions == null) {
      noTransformOptions = new RequestOptions()
          .dontTransform()
          .autoClone();
    }
    return noTransformOptions;
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
  @SuppressWarnings("WeakerAccess") // Public API
  public static RequestOptions formatOf(@NonNull DecodeFormat format) {
    return new RequestOptions().format(format);
  }

  /**
   * Returns a {@link RequestOptions} object with {@link #frame(long)} set.
   */
  @SuppressWarnings("WeakerAccess") // Public API
  public static RequestOptions frameOf(long frameTimeMicros) {
    return new RequestOptions().frame(frameTimeMicros);
  }

  /**
   * Returns a {@link RequestOptions} object with {@link #downsample(DownsampleStrategy)} set.
   */
  @SuppressWarnings("WeakerAccess") // Public API
  public static RequestOptions downsampleOf(@NonNull DownsampleStrategy strategy) {
    return new RequestOptions().downsample(strategy);
  }

  /**
   * Returns a {@link RequestOptions} object with {@link #timeout(int)} set.
   */
  public static RequestOptions timeoutOf(int timeout) {
    return new RequestOptions().timeout(timeout);
  }

  /**
   * Returns a {@link com.bumptech.glide.request.RequestOptions} with {@link
   * #encodeQuality(int)} called with the given quality.
   */
  @SuppressWarnings("WeakerAccess") // Public API
  public static RequestOptions encodeQualityOf(int quality) {
    return new RequestOptions().encodeQuality(quality);
  }

  /**
   * Returns a {@link com.bumptech.glide.request.RequestOptions} with {@link
   * #encodeFormat(android.graphics.Bitmap.CompressFormat)} called with the given format.
   */
  @SuppressWarnings("WeakerAccess") // Public API
  public static RequestOptions encodeFormatOf(@NonNull Bitmap.CompressFormat format) {
    return new RequestOptions().encodeFormat(format);
  }

  /**
   * Returns a new {@link com.bumptech.glide.request.RequestOptions} with {@link #dontAnimate()}
   * called.
   */
  @SuppressWarnings("WeakerAccess") // Public API
  public static RequestOptions noAnimation() {
    if (noAnimationOptions == null) {
      noAnimationOptions = new RequestOptions()
          .dontAnimate()
          .autoClone();
    }
    return noAnimationOptions;
  }

  private static boolean isSet(int fields, int flag) {
    return (fields & flag) != 0;
  }

  /**
   * Applies a multiplier to the {@link com.bumptech.glide.request.target.Target}'s size before
   * loading the resource. Useful for loading thumbnails or trying to avoid loading huge resources
   * (particularly {@link Bitmap}s on devices with overly dense screens.
   *
   * @param sizeMultiplier The multiplier to apply to the
   *                       {@link com.bumptech.glide.request.target.Target}'s dimensions when
   *                       loading the resource.
   * @return This request builder.
   */
  public RequestOptions sizeMultiplier(float sizeMultiplier) {
    if (isAutoCloneEnabled) {
      return clone().sizeMultiplier(sizeMultiplier);
    }

    if (sizeMultiplier < 0f || sizeMultiplier > 1f) {
      throw new IllegalArgumentException("sizeMultiplier must be between 0 and 1");
    }
    this.sizeMultiplier = sizeMultiplier;
    fields |= SIZE_MULTIPLIER;

    return selfOrThrowIfLocked();
  }

  public RequestOptions useUnlimitedSourceGeneratorsPool(boolean flag) {
    if (isAutoCloneEnabled) {
      return clone().useUnlimitedSourceGeneratorsPool(flag);
    }

    this.useUnlimitedSourceGeneratorsPool = flag;
    fields |= USE_UNLIMITED_SOURCE_GENERATORS_POOL;

    return selfOrThrowIfLocked();
  }

  /**
   * If set to true, will only load an item if found in the cache, and will not fetch from source.
   */
  public RequestOptions onlyRetrieveFromCache(boolean flag) {
    if (isAutoCloneEnabled) {
      return clone().onlyRetrieveFromCache(flag);
    }

    this.onlyRetrieveFromCache = flag;
    fields |= ONLY_RETRIEVE_FROM_CACHE;

    return selfOrThrowIfLocked();
  }

  /**
   * Sets the {@link DiskCacheStrategy} to use for this load.
   *
   * <p> Defaults to {@link DiskCacheStrategy#AUTOMATIC}. </p>
   *
   * <p> For most applications {@link DiskCacheStrategy#RESOURCE} is
   * ideal. Applications that use the same resource multiple times in multiple sizes and are willing
   * to trade off some speed and disk space in return for lower bandwidth usage may want to consider
   * using {@link DiskCacheStrategy#DATA} or
   * {@link DiskCacheStrategy#ALL}. </p>
   *
   * @param strategy The strategy to use.
   * @return This request builder.
   */
  public RequestOptions diskCacheStrategy(@NonNull DiskCacheStrategy strategy) {
    if (isAutoCloneEnabled) {
      return clone().diskCacheStrategy(strategy);
    }
    this.diskCacheStrategy = Preconditions.checkNotNull(strategy);
    fields |= DISK_CACHE_STRATEGY;

    return selfOrThrowIfLocked();
  }

  /**
   * Sets the priority for this load.
   *
   * @param priority A priority.
   * @return This request builder.
   */
  public RequestOptions priority(@NonNull Priority priority) {
    if (isAutoCloneEnabled) {
      return clone().priority(priority);
    }

    this.priority = Preconditions.checkNotNull(priority);
    fields |= PRIORITY;

    return selfOrThrowIfLocked();
  }

  /**
   * Sets an {@link Drawable} to display while a resource is loading.
   *
   * @param drawable The drawable to display as a placeholder.
   * @return This request builder.
   */
  public RequestOptions placeholder(@Nullable Drawable drawable) {
    if (isAutoCloneEnabled) {
      return clone().placeholder(drawable);
    }

    this.placeholderDrawable = drawable;
    fields |= PLACEHOLDER;

    return selfOrThrowIfLocked();
  }

  /**
   * Sets an Android resource id for a {@link Drawable} resource to
   * display while a resource is loading.
   *
   * @param resourceId The id of the resource to use as a placeholder
   * @return This request builder.
   */
  public RequestOptions placeholder(int resourceId) {
    if (isAutoCloneEnabled) {
      return clone().placeholder(resourceId);
    }

    this.placeholderId = resourceId;
    fields |= PLACEHOLDER_ID;

    return selfOrThrowIfLocked();
  }

  /**
   * Sets an {@link Drawable} to display if the model provided to
   * {@link com.bumptech.glide.RequestBuilder#load(Object)} is {@code null}.
   *
   * <p> If a fallback is not set, null models will cause the error drawable to be displayed. If the
   * error drawable is not set, the placeholder will be displayed.
   *
   * @see #placeholder(Drawable)
   * @see #placeholder(int)
   *
   * @param drawable The drawable to display as a placeholder.
   * @return This request builder.
   */
  public RequestOptions fallback(Drawable drawable) {
    if (isAutoCloneEnabled) {
      return clone().fallback(drawable);
    }

    this.fallbackDrawable = drawable;
    fields |= FALLBACK;

    return selfOrThrowIfLocked();
  }

  /**
   * Sets a resource to display if the model provided to
   * {@link com.bumptech.glide.RequestBuilder#load(Object)} is {@code null}.
   *
   * <p> If a fallback is not set, null models will cause the error drawable to be displayed. If
   * the error drawable is not set, the placeholder will be displayed.
   *
   * @see #placeholder(Drawable)
   * @see #placeholder(int)
   *
   * @param resourceId The id of the resource to use as a fallback.
   * @return This request builder.
   */
  public RequestOptions fallback(int resourceId) {
    if (isAutoCloneEnabled) {
      return clone().fallback(resourceId);
    }

    this.fallbackId = resourceId;
    fields |= FALLBACK_ID;

    return selfOrThrowIfLocked();
  }

  /**
   * Sets a {@link Drawable} to display if a load fails.
   *
   * @param drawable The drawable to display.
   * @return This request builder.
   */
  public RequestOptions error(@Nullable Drawable drawable) {
    if (isAutoCloneEnabled) {
      return clone().error(drawable);
    }

    this.errorPlaceholder = drawable;
    fields |= ERROR_PLACEHOLDER;

    return selfOrThrowIfLocked();
  }

  /**
   * Sets a resource to display if a load fails.
   *
   * @param resourceId The id of the resource to use as a placeholder.
   * @return This request builder.
   */
  public RequestOptions error(int resourceId) {
    if (isAutoCloneEnabled) {
      return clone().error(resourceId);
    }
    this.errorId = resourceId;
    fields |= ERROR_ID;

    return selfOrThrowIfLocked();
  }

  /**
   * Sets the {@link android.content.res.Resources.Theme} to apply when loading {@link Drawable}s
   * for resource ids provided via {@link #error(int)}, {@link #placeholder(int)}, and
   * {@link #fallback(Drawable)}.
   *
   * @param theme The theme to use when loading Drawables.
   * @return this request builder.
   */
  public RequestOptions theme(Resources.Theme theme) {
    if (isAutoCloneEnabled) {
      return clone().theme(theme);
    }

    this.theme = theme;
    fields |= THEME;

    return selfOrThrowIfLocked();
  }

  /**
   * Allows the loaded resource to skip the memory cache.
   *
   * <p> Note - this is not a guarantee. If a request is already pending for this resource and that
   * request is not also skipping the memory cache, the resource will be cached in memory.</p>
   *
   * @param skip True to allow the resource to skip the memory cache.
   * @return This request builder.
   */
  public RequestOptions skipMemoryCache(boolean skip) {
    if (isAutoCloneEnabled) {
      return clone().skipMemoryCache(true);
    }

    this.isCacheable = !skip;
    fields |= IS_CACHEABLE;

    return selfOrThrowIfLocked();
  }

  /**
   * Overrides the {@link com.bumptech.glide.request.target.Target}'s width and height with the
   * given values. This is useful for thumbnails, and should only be used for other cases when you
   * need a very specific image size.
   *
   * @param width  The width in pixels to use to load the resource.
   * @param height The height in pixels to use to load the resource.
   * @return This request builder.
   */
  public RequestOptions override(int width, int height) {
    if (isAutoCloneEnabled) {
      return clone().override(width, height);
    }

    this.overrideWidth = width;
    this.overrideHeight = height;
    fields |= OVERRIDE;

    return selfOrThrowIfLocked();
  }

  /**
   * Overrides the {@link com.bumptech.glide.request.target.Target}'s width and height with the
   * given size.
   *
   * @see #override(int, int)
   * @param size The width and height to use.
   * @return This request builder.
   */
  public RequestOptions override(int size) {
    return override(size, size);
  }

  /**
   * Sets some additional data to be mixed in to the memory and disk cache keys allowing the caller
   * more control over when cached data is invalidated.
   *
   * <p> Note - The signature does not replace the cache key, it is purely additive. </p>
   *
   * @param signature A unique non-null {@link Key} representing the current
   *                  state of the model that will be mixed in to the cache key.
   * @return This request builder.
   * @see com.bumptech.glide.signature.ObjectKey
   */
  public RequestOptions signature(@NonNull Key signature) {
    if (isAutoCloneEnabled) {
      return clone().signature(signature);
    }

    this.signature = Preconditions.checkNotNull(signature);
    fields |= SIGNATURE;
    return selfOrThrowIfLocked();
  }

  /**
   * Returns a copy of this request builder with all of the options put so far on this builder.
   *
   * <p> This method returns a "deep" copy in that all non-immutable arguments are copied such that
   * changes to one builder will not affect the other builder. However, in addition to immutable
   * arguments, the current model is not copied copied so changes to the model will affect both
   * builders. </p>
   *
   * <p> Even if this object was locked, the cloned object returned from this method will not be
   * locked. </p>
   */
  @SuppressWarnings("unchecked")
  @Override
  public RequestOptions clone() {
    try {
      RequestOptions result = (RequestOptions) super.clone();
      result.options = new Options();
      result.options.putAll(options);
      result.transformations = new HashMap<>();
      result.transformations.putAll(transformations);
      result.isLocked = false;
      result.isAutoCloneEnabled = false;
      return result;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  public <T> RequestOptions set(@NonNull Option<T> option, @NonNull T value) {
    if (isAutoCloneEnabled) {
      return clone().set(option, value);
    }

    Preconditions.checkNotNull(option);
    Preconditions.checkNotNull(value);
    options.set(option, value);
    return selfOrThrowIfLocked();
  }

  public RequestOptions decode(@NonNull Class<?> resourceClass) {
    if (isAutoCloneEnabled) {
      return clone().decode(resourceClass);
    }

    this.resourceClass = Preconditions.checkNotNull(resourceClass);
    fields |= RESOURCE_CLASS;
    return selfOrThrowIfLocked();
  }

  public final boolean isTransformationAllowed() {
    return isTransformationAllowed;
  }

  public final boolean isTransformationSet() {
    return isSet(TRANSFORMATION);
  }

  public final boolean isLocked() {
    return isLocked;
  }

  /**
   * Sets the value for key
   * {@link com.bumptech.glide.load.resource.bitmap.BitmapEncoder#COMPRESSION_FORMAT}.
   */
  public RequestOptions encodeFormat(@NonNull Bitmap.CompressFormat format) {
    return set(BitmapEncoder.COMPRESSION_FORMAT, Preconditions.checkNotNull(format));
  }

  /**
   * Sets the value for key
   * {@link BitmapEncoder#COMPRESSION_QUALITY}.
   */
  public RequestOptions encodeQuality(int quality) {
    return set(BitmapEncoder.COMPRESSION_QUALITY, quality);
  }

  /**
   * Sets the time position of the frame to extract from a video.
   *
   * <p>This is a component option specific to {@link VideoBitmapDecoder}. If the default video
   * decoder is replaced or skipped because of your configuration, this option may be ignored.
   *
   * @see VideoBitmapDecoder#TARGET_FRAME
   * @param frameTimeMicros The time position in microseconds of the desired frame. If negative, the
   *                        Android framework implementation return a representative frame.
   */
  public RequestOptions frame(long frameTimeMicros) {
    return set(VideoBitmapDecoder.TARGET_FRAME, frameTimeMicros);
  }

  /**
   * Sets the {@link DecodeFormat} to use when decoding {@link Bitmap} objects using
   * {@link Downsampler}.
   *
   * <p>{@link DecodeFormat} is a request, not a requirement. It's possible the resource will be
   * decoded using a decoder that cannot control the format
   * ({@link android.media.MediaMetadataRetriever} for example), or that the decoder may choose to
   * ignore the requested format if it can't display the image (i.e. RGB_565 is requested, but the
   * image has alpha).
   *
   * <p>This is a component option specific to {@link Downsampler}. If the defautlt Bitmap decoder
   * is replaced or skipped because of your configuration, this option may be ignored.
   *
   * @see Downsampler#DECODE_FORMAT
   */
  public RequestOptions format(@NonNull DecodeFormat format) {
    return set(Downsampler.DECODE_FORMAT, Preconditions.checkNotNull(format));
  }

  /**
   * Disables the use of {@link android.graphics.Bitmap.Config#HARDWARE} in {@link Downsampler} to
   * avoid errors caused by inspecting Bitmap pixels, drawing with hardware support disabled,
   * drawing to {@link android.graphics.Canvas}s backed by {@link Bitmap}s etc.
   *
   * <p>It's almost never safe to set {@link Downsampler#ALLOW_HARDWARE_CONFIG} to {@code true} so
   * we only provide a way to disable hardware configs entirely. If no option is set for
   * {@link Downsampler#ALLOW_HARDWARE_CONFIG}, Glide will set the value per request based on
   * whether or not a {@link Transformation} is applied and if one is, the type of
   * {@link Transformation} applied. Built in transformations like {@link FitCenter} and
   * {@link com.bumptech.glide.load.resource.bitmap.DownsampleStrategy.CenterOutside} can safely use
   * {@link android.graphics.Bitmap.Config#HARDWARE} because they can be entirely replaced by
   * scaling within {@link Downsampler}. {@link Transformation}s like {@link #circleCrop()} that
   * can't be replicated by {@link Downsampler} cannot use {@link Bitmap.Config#HARDWARE} because
   * {@link android.graphics.Bitmap.Config#HARDWARE} cannot be drawn to
   * {@link android.graphics.Canvas}s, which is required by most {@link Transformation}s.
   */
  public RequestOptions disallowHardwareConfig() {
    return set(Downsampler.ALLOW_HARDWARE_CONFIG, false);
  }

  /**
   * Sets the {@link DownsampleStrategy} to use when decoding {@link Bitmap Bitmaps} using
   * {@link Downsampler}.
   *
   * <p>This is a component option specific to {@link Downsampler}. If the defautlt Bitmap decoder
   * is replaced or skipped because of your configuration, this option may be ignored.
   */
  public RequestOptions downsample(@NonNull DownsampleStrategy strategy) {
    return set(Downsampler.DOWNSAMPLE_STRATEGY, Preconditions.checkNotNull(strategy));
  }

  /**
   * Sets the read and write timeout for the http requests used to load the image.
   *
   * <p>This is a component option specific to Glide's default networking library and
   * {@link com.bumptech.glide.load.model.stream.HttpGlideUrlLoader}. If you use any other
   * networking library including Glide's Volley or OkHttp integration libraries, this option will
   * be ignored.
   *
   * @see com.bumptech.glide.load.model.stream.HttpGlideUrlLoader#TIMEOUT
   * @param timeoutMs The read and write timeout in milliseconds.
   */
  public RequestOptions timeout(int timeoutMs) {
    return set(HttpGlideUrlLoader.TIMEOUT, timeoutMs);
  }

  /**
   * Applies {@link com.bumptech.glide.load.resource.bitmap.CenterCrop} to all default types, and
   * ignores unknown types.
   *
   * <p>This will override previous calls to {@link #dontTransform()}.
   *
   * @see #optionalTransform(Class, Transformation)
   * @see #centerCrop()
   */
  public RequestOptions optionalCenterCrop() {
    return optionalTransform(DownsampleStrategy.CENTER_OUTSIDE, new CenterCrop());
  }

  /**
   * Applies {@link CenterCrop} to all default types and
   * throws an exception if asked to transform an unknown type.
   *
   * <p>this will override previous calls to {@link #dontTransform()} ()}.
   *
   * @see #transform(Class, Transformation)
   * @see #optionalCenterCrop()
   */
  public RequestOptions centerCrop() {
    return transform(DownsampleStrategy.CENTER_OUTSIDE, new CenterCrop());
  }

  /**
   *
   * Applies {@link FitCenter} and to all default types, {@link DownsampleStrategy#FIT_CENTER} to
   * image types, and ignores unknown types.
   *
   * <p>This will override previous calls to {@link #dontTransform()} and previous calls to
   * {@link #downsample(DownsampleStrategy)}.
   *
   * @see #optionalTransform(Class, Transformation)
   * @see #fitCenter()
   */
  public RequestOptions optionalFitCenter() {
    return optionalScaleOnlyTransform(DownsampleStrategy.FIT_CENTER, new FitCenter());
  }

  /**
   * Applies {@link FitCenter} and to all default types, {@link DownsampleStrategy#FIT_CENTER} to
   * image types, and throws an exception if asked to transform an unknown
   * type.
   *
   * <p>This will override previous calls to {@link #dontTransform()} and previous calls to
   * {@link #downsample(DownsampleStrategy)}.
   *
   * @see #transform(Class, Transformation)
   * @see #optionalFitCenter()
   */
  public RequestOptions fitCenter() {
    return scaleOnlyTransform(DownsampleStrategy.FIT_CENTER, new FitCenter());
  }

  /**
   * Applies {@link com.bumptech.glide.load.resource.bitmap.CenterInside} to all default types,
   * {@link DownsampleStrategy#CENTER_INSIDE} to image types, and ignores unknown types.
   *
   * <p>This will override previous calls to {@link #dontTransform()} and previous calls to
   * {@link #downsample(DownsampleStrategy)}.
   *
   * @see #optionalTransform(Class, Transformation)
   * @see #centerInside()
   */
  public RequestOptions optionalCenterInside() {
    return optionalScaleOnlyTransform(DownsampleStrategy.CENTER_INSIDE, new CenterInside());
  }

  /**
   * Applies {@link CenterInside} to all default types, {@link DownsampleStrategy#CENTER_INSIDE} to
   * image types and throws an exception if asked to transform an unknown type.
   *
   * <p>This will override previous calls to {@link #dontTransform()} and previous calls to
   * {@link #downsample(DownsampleStrategy)}.
   *
   * @see #transform(Class, Transformation)
   * @see #optionalCenterInside()
   */
  public RequestOptions centerInside() {
    return scaleOnlyTransform(DownsampleStrategy.CENTER_INSIDE, new CenterInside());
  }

  /**
   * Applies {@link CircleCrop} to all default types, and ignores unknown types.
   *
   * <p>This will override previous calls to {@link #dontTransform()}.
   *
   * @see #optionalTransform(Transformation)
   * @see #circleCrop()
   */
  public RequestOptions optionalCircleCrop() {
    return optionalTransform(DownsampleStrategy.CENTER_OUTSIDE, new CircleCrop());
  }

  /**
   * Applies {@link CircleCrop} to all default types and throws an exception if asked to transform
   * an unknown type.
   *
   * <p>This will override previous calls to {@link #dontTransform()}.
   *
   * @see #transform(Class, Transformation)
   * @see #optionalCenterCrop()
   */
  public RequestOptions circleCrop() {
    return transform(DownsampleStrategy.CENTER_INSIDE, new CircleCrop());
  }

  // calling optionalTransform() on the result of clone() requires greater access.
  @SuppressWarnings("WeakerAccess")
  final RequestOptions optionalTransform(DownsampleStrategy downsampleStrategy,
      Transformation<Bitmap> transformation) {
    if (isAutoCloneEnabled) {
      return clone().optionalTransform(downsampleStrategy, transformation);
    }

    downsample(downsampleStrategy);
    return optionalTransform(transformation);
  }

  // calling transform() on the result of clone() requires greater access.
  @SuppressWarnings("WeakerAccess")
  final RequestOptions transform(DownsampleStrategy downsampleStrategy,
      Transformation<Bitmap> transformation) {
    if (isAutoCloneEnabled) {
      return clone().transform(downsampleStrategy, transformation);
    }

    downsample(downsampleStrategy);
    return transform(transformation);
  }

  private RequestOptions scaleOnlyTransform(
      DownsampleStrategy strategy, Transformation<Bitmap> transformation) {
    return scaleOnlyTransform(strategy, transformation, true /*isTransformationRequired*/);
  }

  private RequestOptions optionalScaleOnlyTransform(
      DownsampleStrategy strategy, Transformation<Bitmap> transformation) {
    return scaleOnlyTransform(strategy, transformation, false /*isTransformationRequired*/);
  }

  private RequestOptions scaleOnlyTransform(
      DownsampleStrategy strategy,
      Transformation<Bitmap> transformation,
      boolean isTransformationRequired) {
    RequestOptions result = isTransformationRequired
          ? transform(strategy, transformation) : optionalTransform(strategy, transformation);
    result.isScaleOnlyOrNoTransform = true;
    return result;
  }

  /**
   * Applies the given {@link Transformation} for
   * {@link Bitmap Bitmaps} to the default types ({@link Bitmap},
   * {@link android.graphics.drawable.BitmapDrawable}, and
   * {@link com.bumptech.glide.load.resource.gif.GifDrawable})
   * and throws an exception if asked to transform an unknown type.
   *
   * <p>This will override previous calls to {@link #dontTransform()}.
   *
   * @param transformation Any {@link Transformation} for {@link Bitmap}s.
   * @see #optionalTransform(Transformation)
   * @see #optionalTransform(Class, Transformation)
   */
  public RequestOptions transform(@NonNull Transformation<Bitmap> transformation) {
    if (isAutoCloneEnabled) {
      return clone().transform(transformation);
    }

    optionalTransform(transformation);
    isTransformationRequired = true;
    fields |= TRANSFORMATION_REQUIRED;
    return selfOrThrowIfLocked();
  }

  /**
   * Applies the given {@link Transformation}s in the given order for
   * {@link Bitmap Bitmaps} to the default types ({@link Bitmap},
   * {@link android.graphics.drawable.BitmapDrawable}, and
   * {@link com.bumptech.glide.load.resource.gif.GifDrawable})
   * and throws an exception if asked to transform an unknown type.
   *
   * <p>This will override previous calls to {@link #dontTransform()}.
   *
   * @param transformations One or more {@link Transformation}s for {@link Bitmap}s.
   * @see #optionalTransform(Transformation)
   * @see #optionalTransform(Class, Transformation)
   */
  @SuppressWarnings({"unchecked", "varargs"})
  public RequestOptions transforms(@NonNull Transformation<Bitmap>... transformations) {
    if (isAutoCloneEnabled) {
      return clone().transforms(transformations);
    }

    optionalTransform(new MultiTransformation<>(transformations));
    isTransformationRequired = true;
    fields |= TRANSFORMATION_REQUIRED;
    return selfOrThrowIfLocked();
  }

  /**
   * Applies the given {@link Transformation} for
   * {@link Bitmap Bitmaps} to the default types ({@link Bitmap},
   * {@link android.graphics.drawable.BitmapDrawable}, and
   * {@link com.bumptech.glide.load.resource.gif.GifDrawable}) and ignores unknown types.
   *
   * <p>This will override previous calls to {@link #dontTransform()}.
   *
   * @param transformation Any {@link Transformation} for {@link Bitmap}s.
   * @see #transform(Transformation)
   * @see #transform(Class, Transformation)
   */
  public RequestOptions optionalTransform(Transformation<Bitmap> transformation) {
    if (isAutoCloneEnabled) {
      return clone().optionalTransform(transformation);
    }

    optionalTransform(Bitmap.class, transformation);
    // TODO: remove BitmapDrawable decoder and this transformation.
    optionalTransform(BitmapDrawable.class,
        new BitmapDrawableTransformation(transformation));
    optionalTransform(GifDrawable.class, new GifDrawableTransformation(transformation));
    return selfOrThrowIfLocked();
  }

  /**
   * Applies the given {@link Transformation} for any decoded resource of
   * the given type and allows unknown resource types to be ignored.
   *
   * <p> Users can apply different transformations for each resource class. Applying a
   * {@link Transformation} for a resource type that already has a
   * {@link Transformation} will override the previous call. </p>
   *
   * <p> If any calls are made to the non-optional transform methods, then attempting to transform
   * an unknown resource class will throw an exception. To allow unknown types, users must always
   * call the optional version of each method. </p>
   *
   * <p>This will override previous calls to {@link #dontTransform()}.
   *
   * @param resourceClass  The type of resource to transform.
   * @param transformation The {@link Transformation} to apply.
   */
  public <T> RequestOptions optionalTransform(Class<T> resourceClass,
      Transformation<T> transformation) {
    if (isAutoCloneEnabled) {
      return clone().optionalTransform(resourceClass, transformation);
    }

    Preconditions.checkNotNull(resourceClass);
    Preconditions.checkNotNull(transformation);
    transformations.put(resourceClass, transformation);
    fields |= TRANSFORMATION;
    isTransformationAllowed = true;
    fields |= TRANSFORMATION_ALLOWED;
    // Always set to false here. Known scale only transformations will call this method and then
    // set isScaleOnlyOrNoTransform to true immediately after.
    isScaleOnlyOrNoTransform = false;
    return selfOrThrowIfLocked();
  }

  /**
   * Applies the given {@link Transformation} for any decoded resource of
   * the given type and throws if asked to transform an unknown resource type.
   *
   * <p>This will override previous calls to {@link #dontTransform()}.
   *
   * @param resourceClass  The type of resource to transform.
   * @param transformation The {@link Transformation} to apply.
   * @see #optionalTransform(Class, Transformation)
   */
  public <T> RequestOptions transform(
      Class<T> resourceClass, Transformation<T> transformation) {
    if (isAutoCloneEnabled) {
      return clone().transform(resourceClass, transformation);
    }

    optionalTransform(resourceClass, transformation);
    isTransformationRequired = true;
    fields |= TRANSFORMATION_REQUIRED;
    return selfOrThrowIfLocked();
  }

  /**
   * Removes all applied {@link Transformation Transformations} for all
   * resource classes and allows unknown resource types to be transformed without throwing an
   * exception.
   */
  public RequestOptions dontTransform() {
    if (isAutoCloneEnabled) {
      return clone().dontTransform();
    }

    transformations.clear();
    fields &= ~TRANSFORMATION;
    isTransformationRequired = false;
    fields &= ~TRANSFORMATION_REQUIRED;
    isTransformationAllowed = false;
    fields |= TRANSFORMATION_ALLOWED;
    isScaleOnlyOrNoTransform = true;
    return selfOrThrowIfLocked();
  }

  /**
   * Disables resource decoders that return animated resources so any resource returned will be
   * static.
   *
   * <p> To disable transitions (fades etc) use
   * {@link com.bumptech.glide.TransitionOptions#dontTransition()}</p>
   */
  public RequestOptions dontAnimate() {
    if (isAutoCloneEnabled) {
      return clone().dontAnimate();
    }

    set(ByteBufferGifDecoder.DISABLE_ANIMATION, true);
    set(StreamGifDecoder.DISABLE_ANIMATION, true);
    return selfOrThrowIfLocked();
  }

  public RequestOptions apply(RequestOptions other) {
    if (isAutoCloneEnabled) {
      return clone().apply(other);
    }

    if (isSet(other.fields, SIZE_MULTIPLIER)) {
      sizeMultiplier = other.sizeMultiplier;
    }
    if (isSet(other.fields, USE_UNLIMITED_SOURCE_GENERATORS_POOL)) {
      useUnlimitedSourceGeneratorsPool = other.useUnlimitedSourceGeneratorsPool;
    }
    if (isSet(other.fields, DISK_CACHE_STRATEGY)) {
      diskCacheStrategy = other.diskCacheStrategy;
    }
    if (isSet(other.fields, PRIORITY)) {
      priority = other.priority;
    }
    if (isSet(other.fields, ERROR_PLACEHOLDER)) {
      errorPlaceholder = other.errorPlaceholder;
    }
    if (isSet(other.fields, ERROR_ID)) {
      errorId = other.errorId;
    }
    if (isSet(other.fields, PLACEHOLDER)) {
      placeholderDrawable = other.placeholderDrawable;
    }
    if (isSet(other.fields, PLACEHOLDER_ID)) {
      placeholderId = other.placeholderId;
    }
    if (isSet(other.fields, IS_CACHEABLE)) {
      isCacheable = other.isCacheable;
    }
    if (isSet(other.fields, OVERRIDE)) {
      overrideWidth = other.overrideWidth;
      overrideHeight = other.overrideHeight;
    }
    if (isSet(other.fields, SIGNATURE)) {
      signature = other.signature;
    }
    if (isSet(other.fields, RESOURCE_CLASS)) {
      resourceClass = other.resourceClass;
    }
    if (isSet(other.fields, FALLBACK)) {
      fallbackDrawable = other.fallbackDrawable;
    }
    if (isSet(other.fields, FALLBACK_ID)) {
      fallbackId = other.fallbackId;
    }
    if (isSet(other.fields, THEME)) {
      theme = other.theme;
    }
    if (isSet(other.fields, TRANSFORMATION_ALLOWED)) {
      isTransformationAllowed = other.isTransformationAllowed;
    }
    if (isSet(other.fields, TRANSFORMATION_REQUIRED)) {
      isTransformationRequired = other.isTransformationRequired;
    }
    if (isSet(other.fields, TRANSFORMATION)) {
      transformations.putAll(other.transformations);
      isScaleOnlyOrNoTransform = other.isScaleOnlyOrNoTransform;
    }
    if (isSet(other.fields, ONLY_RETRIEVE_FROM_CACHE)) {
      onlyRetrieveFromCache = other.onlyRetrieveFromCache;
    }

    // Applying options with dontTransform() is expected to clear our transformations.
    if (!isTransformationAllowed) {
      transformations.clear();
      fields &= ~TRANSFORMATION;
      isTransformationRequired = false;
      fields &= ~TRANSFORMATION_REQUIRED;
      isScaleOnlyOrNoTransform = true;
    }

    fields |= other.fields;
    options.putAll(other.options);

    return selfOrThrowIfLocked();
  }


  @Override
  public boolean equals(Object o) {
    if (o instanceof RequestOptions) {
      RequestOptions other = (RequestOptions) o;
      return Float.compare(other.sizeMultiplier, sizeMultiplier) == 0
          && errorId == other.errorId
          && Util.bothNullOrEqual(errorPlaceholder, other.errorPlaceholder)
          && placeholderId == other.placeholderId
          && Util.bothNullOrEqual(placeholderDrawable, other.placeholderDrawable)
          && fallbackId == other.fallbackId
          && Util.bothNullOrEqual(fallbackDrawable, other.fallbackDrawable)
          && isCacheable == other.isCacheable
          && overrideHeight == other.overrideHeight
          && overrideWidth == other.overrideWidth
          && isTransformationRequired == other.isTransformationRequired
          && isTransformationAllowed == other.isTransformationAllowed
          && useUnlimitedSourceGeneratorsPool == other.useUnlimitedSourceGeneratorsPool
          && onlyRetrieveFromCache == other.onlyRetrieveFromCache
          && diskCacheStrategy.equals(other.diskCacheStrategy)
          && priority == other.priority
          && options.equals(other.options)
          && transformations.equals(other.transformations)
          && resourceClass.equals(other.resourceClass)
          && Util.bothNullOrEqual(signature, other.signature)
          && Util.bothNullOrEqual(theme, other.theme);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hashCode = Util.hashCode(sizeMultiplier);
    hashCode = Util.hashCode(errorId, hashCode);
    hashCode = Util.hashCode(errorPlaceholder, hashCode);
    hashCode = Util.hashCode(placeholderId, hashCode);
    hashCode = Util.hashCode(placeholderDrawable, hashCode);
    hashCode = Util.hashCode(fallbackId, hashCode);
    hashCode = Util.hashCode(fallbackDrawable, hashCode);
    hashCode = Util.hashCode(isCacheable, hashCode);
    hashCode = Util.hashCode(overrideHeight, hashCode);
    hashCode = Util.hashCode(overrideWidth, hashCode);
    hashCode = Util.hashCode(isTransformationRequired, hashCode);
    hashCode = Util.hashCode(isTransformationAllowed, hashCode);
    hashCode = Util.hashCode(useUnlimitedSourceGeneratorsPool, hashCode);
    hashCode = Util.hashCode(onlyRetrieveFromCache, hashCode);
    hashCode = Util.hashCode(diskCacheStrategy, hashCode);
    hashCode = Util.hashCode(priority, hashCode);
    hashCode = Util.hashCode(options, hashCode);
    hashCode = Util.hashCode(transformations, hashCode);
    hashCode = Util.hashCode(resourceClass, hashCode);
    hashCode = Util.hashCode(signature, hashCode);
    hashCode = Util.hashCode(theme, hashCode);
    return hashCode;
  }

  /**
   * Throws if any further mutations are attempted.
   *
   * <p> Once locked, the only way to unlock is to use {@link #clone()} </p>
   */
  @SuppressWarnings("unchecked")
  public RequestOptions lock() {
    isLocked = true;
    // This is the only place we should not check locked.
    return this;
  }

  /**
   * Similar to {@link #lock()} except that mutations cause a {@link #clone()} operation to happen
   * before the mutation resulting in all methods returning a new Object and leaving the original
   * locked object unmodified.
   *
   * <p>Auto clone is not retained by cloned objects returned from mutations. The cloned objects
   * are mutable and are not locked.
   */
  public RequestOptions autoClone() {
    if (isLocked && !isAutoCloneEnabled) {
      throw new IllegalStateException("You cannot auto lock an already locked options object"
          + ", try clone() first");
    }
    isAutoCloneEnabled = true;
    return lock();
  }

  @SuppressWarnings("unchecked")
  private RequestOptions selfOrThrowIfLocked() {
    if (isLocked) {
      throw new IllegalStateException("You cannot modify locked RequestOptions, consider clone()");
    }
    return this;
  }

  protected boolean isAutoCloneEnabled() {
    return isAutoCloneEnabled;
  }

  @NonNull
  public final Map<Class<?>, Transformation<?>> getTransformations() {
    return transformations;
  }

  @SuppressWarnings("WeakerAccess")
  public final boolean isTransformationRequired() {
    return isTransformationRequired;
  }

  @NonNull
  public final Options getOptions() {
    return options;
  }

  @NonNull
  public final Class<?> getResourceClass() {
    return resourceClass;
  }

  @NonNull
  public final DiskCacheStrategy getDiskCacheStrategy() {
    return diskCacheStrategy;
  }

  @Nullable
  @SuppressWarnings("WeakerAccess")
  public final Drawable getErrorPlaceholder() {
    return errorPlaceholder;
  }

  @SuppressWarnings("WeakerAccess")
  public final int getErrorId() {
    return errorId;
  }

  @SuppressWarnings("WeakerAccess")
  public final int getPlaceholderId() {
    return placeholderId;
  }

  @Nullable
  @SuppressWarnings("WeakerAccess")
  public final Drawable getPlaceholderDrawable() {
    return placeholderDrawable;
  }

  @SuppressWarnings("WeakerAccess")
  public final int getFallbackId() {
    return fallbackId;
  }

  @Nullable
  @SuppressWarnings("WeakerAccess")
  public final Drawable getFallbackDrawable() {
    return fallbackDrawable;
  }

  @Nullable
  public final Resources.Theme getTheme() {
    return theme;
  }

  @SuppressWarnings("WeakerAccess")
  public final boolean isMemoryCacheable() {
    return isCacheable;
  }

  @NonNull
  public final Key getSignature() {
    return signature;
  }

  public final boolean isPrioritySet() {
    return isSet(PRIORITY);
  }

  @NonNull
  public final Priority getPriority() {
    return priority;
  }

  public final int getOverrideWidth() {
    return overrideWidth;
  }

  public final boolean isValidOverride() {
    return Util.isValidDimensions(overrideWidth, overrideHeight);
  }

  public final int getOverrideHeight() {
    return overrideHeight;
  }

  public final float getSizeMultiplier() {
    return sizeMultiplier;
  }

  public boolean isScaleOnlyOrNoTransform() {
    return isScaleOnlyOrNoTransform;
  }

  private boolean isSet(int flag) {
    return isSet(fields, flag);
  }

  public final boolean getUseUnlimitedSourceGeneratorsPool() {
    return useUnlimitedSourceGeneratorsPool;
  }

  public final boolean getOnlyRetrieveFromCache() {
    return onlyRetrieveFromCache;
  }
}
