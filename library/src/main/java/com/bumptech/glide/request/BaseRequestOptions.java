package com.bumptech.glide.request;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
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
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.stream.HttpGlideUrlLoader;
import com.bumptech.glide.load.resource.bitmap.BitmapEncoder;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.CenterInside;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy;
import com.bumptech.glide.load.resource.bitmap.Downsampler;
import com.bumptech.glide.load.resource.bitmap.DrawableTransformation;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.bitmap.VideoDecoder;
import com.bumptech.glide.load.resource.drawable.ResourceDrawableDecoder;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.load.resource.gif.GifDrawableTransformation;
import com.bumptech.glide.load.resource.gif.GifOptions;
import com.bumptech.glide.signature.EmptySignature;
import com.bumptech.glide.util.CachedHashCodeArrayMap;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Util;
import java.util.Map;

/**
 * A base object to allow method sharing between {@link RequestOptions} and {@link
 * com.bumptech.glide.RequestBuilder}.
 *
 * <p>This class is not meant for general use and may change at any time.
 *
 * @param <T> The particular child implementation
 */
@SuppressWarnings({"PMD.UseUtilityClass", "unused"})
public abstract class BaseRequestOptions<T extends BaseRequestOptions<T>> implements Cloneable {
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
  private static final int USE_ANIMATION_POOL = 1 << 20;

  private int fields;
  private float sizeMultiplier = 1f;
  @NonNull private DiskCacheStrategy diskCacheStrategy = DiskCacheStrategy.AUTOMATIC;
  @NonNull private Priority priority = Priority.NORMAL;
  @Nullable private Drawable errorPlaceholder;
  private int errorId;
  @Nullable private Drawable placeholderDrawable;
  private int placeholderId;
  private boolean isCacheable = true;
  private int overrideHeight = UNSET;
  private int overrideWidth = UNSET;
  @NonNull private Key signature = EmptySignature.obtain();
  private boolean isTransformationRequired;
  private boolean isTransformationAllowed = true;
  @Nullable private Drawable fallbackDrawable;
  private int fallbackId;
  @NonNull private Options options = new Options();

  @NonNull
  private Map<Class<?>, Transformation<?>> transformations = new CachedHashCodeArrayMap<>();

  @NonNull private Class<?> resourceClass = Object.class;
  private boolean isLocked;
  @Nullable private Resources.Theme theme;
  private boolean isAutoCloneEnabled;
  private boolean useUnlimitedSourceGeneratorsPool;
  private boolean onlyRetrieveFromCache;
  private boolean isScaleOnlyOrNoTransform = true;
  private boolean useAnimationPool;

  private static boolean isSet(int fields, int flag) {
    return (fields & flag) != 0;
  }

  /**
   * Applies a multiplier to the {@link com.bumptech.glide.request.target.Target}'s size before
   * loading the resource. Useful for loading thumbnails or trying to avoid loading huge resources
   * (particularly {@link Bitmap}s on devices with overly dense screens.
   *
   * @param sizeMultiplier The multiplier to apply to the {@link
   *     com.bumptech.glide.request.target.Target}'s dimensions when loading the resource.
   * @return This request builder.
   */
  @NonNull
  @CheckResult
  public T sizeMultiplier(@FloatRange(from = 0, to = 1) float sizeMultiplier) {
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

  /**
   * If set to {@code true}, uses a cached unlimited {@link java.util.concurrent.Executor} to run
   * the request.
   *
   * <p>This method should <em>ONLY</em> be used when a Glide load is started recursively on one of
   * Glide's threads as part of another request. Using this method in other scenarios can lead to
   * excessive memory usage and OOMs and/or a significant decrease in performance across an
   * application.
   *
   * <p>If both this method and {@link #useAnimationPool(boolean)} are set, this method will be
   * preferred and {@link #useAnimationPool(boolean)} will be ignored.
   */
  @NonNull
  @CheckResult
  public T useUnlimitedSourceGeneratorsPool(boolean flag) {
    if (isAutoCloneEnabled) {
      return clone().useUnlimitedSourceGeneratorsPool(flag);
    }

    this.useUnlimitedSourceGeneratorsPool = flag;
    fields |= USE_UNLIMITED_SOURCE_GENERATORS_POOL;

    return selfOrThrowIfLocked();
  }

  /**
   * If set to {@code true}, uses a special {@link java.util.concurrent.Executor} that is used
   * exclusively for decoding frames of animated resources, like GIFs.
   *
   * <p>The animation executor disallows network operations and must not be used for loads that may
   * load remote data. The animation executor has fewer threads available to it than Glide's normal
   * executors and is only useful as a way of avoiding blocking on longer and more expensive reads
   * for critical requests like those in an animating GIF.
   *
   * <p>If both {@link #useUnlimitedSourceGeneratorsPool(boolean)} and this method are set, {@link
   * #useUnlimitedSourceGeneratorsPool(boolean)} will be preferred and this method will be ignored.
   */
  @NonNull
  @CheckResult
  public T useAnimationPool(boolean flag) {
    if (isAutoCloneEnabled) {
      return clone().useAnimationPool(flag);
    }

    useAnimationPool = flag;
    fields |= USE_ANIMATION_POOL;

    return selfOrThrowIfLocked();
  }

  /**
   * If set to true, will only load an item if found in the cache, and will not fetch from source.
   *
   * <p>By 'cache' we mean both the in memory cache and both types of disk cache ({@link
   * DiskCacheStrategy#DATA} and {@link DiskCacheStrategy#RESOURCE}). If this flag is set to {@code
   * true} and the item is not in the memory cache, but it is in one of the disk caches, the load
   * will complete asynchronously.
   *
   * <p>If you'd like to only load an item from the memory cache. You can call this method with
   * {@code true} and also call {@link #diskCacheStrategy(DiskCacheStrategy)} with {@link
   * DiskCacheStrategy#NONE}
   */
  @NonNull
  @CheckResult
  public T onlyRetrieveFromCache(boolean flag) {
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
   * <p>Defaults to {@link DiskCacheStrategy#AUTOMATIC}.
   *
   * <p>For most applications {@link DiskCacheStrategy#RESOURCE} is ideal. Applications that use the
   * same resource multiple times in multiple sizes and are willing to trade off some speed and disk
   * space in return for lower bandwidth usage may want to consider using {@link
   * DiskCacheStrategy#DATA} or {@link DiskCacheStrategy#ALL}.
   *
   * @param strategy The strategy to use.
   * @return This request builder.
   */
  @NonNull
  @CheckResult
  public T diskCacheStrategy(@NonNull DiskCacheStrategy strategy) {
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
  @NonNull
  @CheckResult
  public T priority(@NonNull Priority priority) {
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
   * <p>Replaces any previous calls to this method or {@link #placeholder(int)}.
   *
   * @param drawable The drawable to display as a placeholder.
   * @return This request builder.
   */
  @NonNull
  @CheckResult
  public T placeholder(@Nullable Drawable drawable) {
    if (isAutoCloneEnabled) {
      return clone().placeholder(drawable);
    }

    this.placeholderDrawable = drawable;
    fields |= PLACEHOLDER;

    placeholderId = 0;
    fields &= ~PLACEHOLDER_ID;

    return selfOrThrowIfLocked();
  }

  /**
   * Sets an Android resource id for a {@link Drawable} resource to display while a resource is
   * loading.
   *
   * <p>Replaces any previous calls to this method or {@link #placeholder(Drawable)}
   *
   * @param resourceId The id of the resource to use as a placeholder
   * @return This request builder.
   */
  @NonNull
  @CheckResult
  public T placeholder(@DrawableRes int resourceId) {
    if (isAutoCloneEnabled) {
      return clone().placeholder(resourceId);
    }

    this.placeholderId = resourceId;
    fields |= PLACEHOLDER_ID;

    placeholderDrawable = null;
    fields &= ~PLACEHOLDER;

    return selfOrThrowIfLocked();
  }

  /**
   * Sets an {@link Drawable} to display if the model provided to {@link
   * com.bumptech.glide.RequestBuilder#load(Object)} is {@code null}.
   *
   * <p>If a fallback is not set, null models will cause the error drawable to be displayed. If the
   * error drawable is not set, the placeholder will be displayed.
   *
   * <p>Replaces any previous calls to this method or {@link #fallback(int)}.
   *
   * @see #placeholder(Drawable)
   * @see #placeholder(int)
   * @param drawable The drawable to display as a placeholder.
   * @return This request builder.
   */
  @NonNull
  @CheckResult
  public T fallback(@Nullable Drawable drawable) {
    if (isAutoCloneEnabled) {
      return clone().fallback(drawable);
    }

    this.fallbackDrawable = drawable;
    fields |= FALLBACK;

    fallbackId = 0;
    fields &= ~FALLBACK_ID;

    return selfOrThrowIfLocked();
  }

  /**
   * Sets a resource to display if the model provided to {@link
   * com.bumptech.glide.RequestBuilder#load(Object)} is {@code null}.
   *
   * <p>If a fallback is not set, null models will cause the error drawable to be displayed. If the
   * error drawable is not set, the placeholder will be displayed.
   *
   * <p>Replaces any previous calls to this method or {@link #fallback(Drawable)}.
   *
   * @see #placeholder(Drawable)
   * @see #placeholder(int)
   * @param resourceId The id of the resource to use as a fallback.
   * @return This request builder.
   */
  @NonNull
  @CheckResult
  public T fallback(@DrawableRes int resourceId) {
    if (isAutoCloneEnabled) {
      return clone().fallback(resourceId);
    }

    this.fallbackId = resourceId;
    fields |= FALLBACK_ID;

    fallbackDrawable = null;
    fields &= ~FALLBACK;

    return selfOrThrowIfLocked();
  }

  /**
   * Sets a {@link Drawable} to display if a load fails.
   *
   * <p>Replaces any previous calls to this method or {@link #error(int)}
   *
   * @param drawable The drawable to display.
   * @return This request builder.
   */
  @NonNull
  @CheckResult
  public T error(@Nullable Drawable drawable) {
    if (isAutoCloneEnabled) {
      return clone().error(drawable);
    }

    this.errorPlaceholder = drawable;
    fields |= ERROR_PLACEHOLDER;

    this.errorId = 0;
    fields &= ~ERROR_ID;

    return selfOrThrowIfLocked();
  }

  /**
   * Sets a resource to display if a load fails.
   *
   * <p>Replaces any previous calls to this method or {@link #error(Drawable)}
   *
   * @param resourceId The id of the resource to use as a placeholder.
   * @return This request builder.
   */
  @NonNull
  @CheckResult
  public T error(@DrawableRes int resourceId) {
    if (isAutoCloneEnabled) {
      return clone().error(resourceId);
    }
    this.errorId = resourceId;
    fields |= ERROR_ID;

    this.errorPlaceholder = null;
    fields &= ~ERROR_PLACEHOLDER;

    return selfOrThrowIfLocked();
  }

  /**
   * Sets the {@link android.content.res.Resources.Theme} to apply when loading {@link Drawable}s
   * for resource ids, including those provided via {@link #error(int)}, {@link #placeholder(int)},
   * and {@link #fallback(Drawable)}.
   *
   * <p>The {@link android.content.res.Resources.Theme} provided here will override the {@link
   * android.content.res.Resources.Theme} of the application {@link android.content.Context}.
   *
   * @param theme The theme to use when loading Drawables.
   * @return this request builder.
   */
  @NonNull
  @CheckResult
  public T theme(@Nullable Resources.Theme theme) {
    if (isAutoCloneEnabled) {
      return clone().theme(theme);
    }
    this.theme = theme;
    if (theme != null) {
      fields |= THEME;
      return set(ResourceDrawableDecoder.THEME, theme);
    } else {
      fields &= ~THEME;
      return removeOption(ResourceDrawableDecoder.THEME);
    }
  }

  /**
   * Allows the loaded resource to skip the memory cache.
   *
   * <p>Note - this is not a guarantee. If a request is already pending for this resource and that
   * request is not also skipping the memory cache, the resource will be cached in memory.
   *
   * @param skip True to allow the resource to skip the memory cache.
   * @return This request builder.
   */
  @NonNull
  @CheckResult
  public T skipMemoryCache(boolean skip) {
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
   * @param width The width in pixels to use to load the resource.
   * @param height The height in pixels to use to load the resource.
   * @return This request builder.
   */
  @NonNull
  @CheckResult
  public T override(int width, int height) {
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
  @NonNull
  @CheckResult
  public T override(int size) {
    return override(size, size);
  }

  /**
   * Sets some additional data to be mixed in to the memory and disk cache keys allowing the caller
   * more control over when cached data is invalidated.
   *
   * <p>Note - The signature does not replace the cache key, it is purely additive.
   *
   * @param signature A unique non-null {@link Key} representing the current state of the model that
   *     will be mixed in to the cache key.
   * @return This request builder.
   * @see com.bumptech.glide.signature.ObjectKey
   */
  @NonNull
  @CheckResult
  public T signature(@NonNull Key signature) {
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
   * <p>This method returns a "deep" copy in that all non-immutable arguments are copied such that
   * changes to one builder will not affect the other builder. However, in addition to immutable
   * arguments, the current model is not copied copied so changes to the model will affect both
   * builders.
   *
   * <p>Even if this object was locked, the cloned object returned from this method will not be
   * locked.
   */
  @SuppressWarnings({
    "unchecked",
    // we don't want to throw to be user friendly
    "PMD.CloneThrowsCloneNotSupportedException",
    // The types we're using here do this automatically.
    "PMD.CloneMethodReturnTypeMustMatchClassName"
  })
  @CheckResult
  @Override
  public T clone() {
    try {
      BaseRequestOptions<?> result = (BaseRequestOptions<?>) super.clone();
      result.options = new Options();
      result.options.putAll(options);
      result.transformations = new CachedHashCodeArrayMap<>();
      result.transformations.putAll(transformations);
      result.isLocked = false;
      result.isAutoCloneEnabled = false;
      return (T) result;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  @NonNull
  @CheckResult
  public <Y> T set(@NonNull Option<Y> option, @NonNull Y value) {
    if (isAutoCloneEnabled) {
      return clone().set(option, value);
    }

    Preconditions.checkNotNull(option);
    Preconditions.checkNotNull(value);
    options.set(option, value);
    return selfOrThrowIfLocked();
  }

  T removeOption(@NonNull Option<?> option) {
    if (isAutoCloneEnabled) {
      return clone().removeOption(option);
    }
    options.remove(option);
    return selfOrThrowIfLocked();
  }

  @NonNull
  @CheckResult
  public T decode(@NonNull Class<?> resourceClass) {
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
   * Sets the value for key {@link
   * com.bumptech.glide.load.resource.bitmap.BitmapEncoder#COMPRESSION_FORMAT}.
   */
  @NonNull
  @CheckResult
  public T encodeFormat(@NonNull Bitmap.CompressFormat format) {
    return set(BitmapEncoder.COMPRESSION_FORMAT, Preconditions.checkNotNull(format));
  }

  /** Sets the value for key {@link BitmapEncoder#COMPRESSION_QUALITY}. */
  @NonNull
  @CheckResult
  public T encodeQuality(@IntRange(from = 0, to = 100) int quality) {
    return set(BitmapEncoder.COMPRESSION_QUALITY, quality);
  }

  /**
   * Sets the time position of the frame to extract from a video.
   *
   * <p>This is a component option specific to {@link VideoDecoder}. If the default video decoder is
   * replaced or skipped because of your configuration, this option may be ignored.
   *
   * @see VideoDecoder#TARGET_FRAME
   * @param frameTimeMicros The time position in microseconds of the desired frame. If negative, the
   *     Android framework implementation return a representative frame.
   */
  @NonNull
  @CheckResult
  public T frame(@IntRange(from = 0) long frameTimeMicros) {
    return set(VideoDecoder.TARGET_FRAME, frameTimeMicros);
  }

  /**
   * Sets the {@link DecodeFormat} to use when decoding {@link Bitmap} objects using {@link
   * Downsampler} and Glide's default GIF decoders.
   *
   * <p>{@link DecodeFormat} is a request, not a requirement. It's possible the resource will be
   * decoded using a decoder that cannot control the format ({@link
   * android.media.MediaMetadataRetriever} for example), or that the decoder may choose to ignore
   * the requested format if it can't display the image (i.e. RGB_565 is requested, but the image
   * has alpha).
   *
   * <p>This is a component option specific to {@link Downsampler} and Glide's GIF decoders. If the
   * default Bitmap decoders are replaced or skipped because of your configuration, this option may
   * be ignored.
   *
   * <p>To set only the format used when decoding {@link Bitmap}s, use {@link #set(Option, Object)}}
   * and {@link Downsampler#DECODE_FORMAT}. To set only the format used when decoding GIF frames,
   * use {@link #set(Option, Object)} and {@link GifOptions#DECODE_FORMAT}.
   *
   * @see Downsampler#DECODE_FORMAT
   * @see GifOptions#DECODE_FORMAT
   */
  @NonNull
  @CheckResult
  public T format(@NonNull DecodeFormat format) {
    Preconditions.checkNotNull(format);
    return set(Downsampler.DECODE_FORMAT, format).set(GifOptions.DECODE_FORMAT, format);
  }

  /**
   * Disables the use of {@link android.graphics.Bitmap.Config#HARDWARE} in {@link Downsampler} to
   * avoid errors caused by inspecting Bitmap pixels, drawing with hardware support disabled,
   * drawing to {@link android.graphics.Canvas}s backed by {@link Bitmap}s etc.
   *
   * <p>It's almost never safe to set {@link Downsampler#ALLOW_HARDWARE_CONFIG} to {@code true} so
   * we only provide a way to disable hardware configs entirely. If no option is set for {@link
   * Downsampler#ALLOW_HARDWARE_CONFIG}, Glide will set the value per request based on whether or
   * not a {@link Transformation} is applied and if one is, the type of {@link Transformation}
   * applied. Built in transformations like {@link FitCenter} and {@link
   * com.bumptech.glide.load.resource.bitmap.DownsampleStrategy.CenterOutside} can safely use {@link
   * android.graphics.Bitmap.Config#HARDWARE} because they can be entirely replaced by scaling
   * within {@link Downsampler}. {@link Transformation}s like {@link #circleCrop()} that can't be
   * replicated by {@link Downsampler} cannot use {@link Bitmap.Config#HARDWARE} because {@link
   * android.graphics.Bitmap.Config#HARDWARE} cannot be drawn to {@link android.graphics.Canvas}s,
   * which is required by most {@link Transformation}s.
   */
  @NonNull
  @CheckResult
  public T disallowHardwareConfig() {
    return set(Downsampler.ALLOW_HARDWARE_CONFIG, false);
  }

  /**
   * Sets the {@link DownsampleStrategy} to use when decoding {@link Bitmap Bitmaps} using {@link
   * Downsampler}.
   *
   * <p>This is a component option specific to {@link Downsampler}. If the defautlt Bitmap decoder
   * is replaced or skipped because of your configuration, this option may be ignored.
   */
  @NonNull
  @CheckResult
  public T downsample(@NonNull DownsampleStrategy strategy) {
    return set(DownsampleStrategy.OPTION, Preconditions.checkNotNull(strategy));
  }

  /**
   * Sets the read and write timeout for the http requests used to load the image.
   *
   * <p>This is a component option specific to Glide's default networking library and {@link
   * com.bumptech.glide.load.model.stream.HttpGlideUrlLoader}. If you use any other networking
   * library including Glide's Volley or OkHttp integration libraries, this option will be ignored.
   *
   * @see com.bumptech.glide.load.model.stream.HttpGlideUrlLoader#TIMEOUT
   * @param timeoutMs The read and write timeout in milliseconds.
   */
  @NonNull
  @CheckResult
  public T timeout(@IntRange(from = 0) int timeoutMs) {
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
  @NonNull
  @CheckResult
  public T optionalCenterCrop() {
    return optionalTransform(DownsampleStrategy.CENTER_OUTSIDE, new CenterCrop());
  }

  /**
   * Applies {@link CenterCrop} to all default types and throws an exception if asked to transform
   * an unknown type.
   *
   * <p>this will override previous calls to {@link #dontTransform()} ()}.
   *
   * @see #transform(Class, Transformation)
   * @see #optionalCenterCrop()
   */
  @NonNull
  @CheckResult
  public T centerCrop() {
    return transform(DownsampleStrategy.CENTER_OUTSIDE, new CenterCrop());
  }

  /**
   * Applies {@link FitCenter} and to all default types, {@link DownsampleStrategy#FIT_CENTER} to
   * image types, and ignores unknown types.
   *
   * <p>This will override previous calls to {@link #dontTransform()} and previous calls to {@link
   * #downsample(DownsampleStrategy)}.
   *
   * @see #optionalTransform(Class, Transformation)
   * @see #fitCenter()
   */
  @NonNull
  @CheckResult
  public T optionalFitCenter() {
    return optionalScaleOnlyTransform(DownsampleStrategy.FIT_CENTER, new FitCenter());
  }

  /**
   * Applies {@link FitCenter} and to all default types, {@link DownsampleStrategy#FIT_CENTER} to
   * image types, and throws an exception if asked to transform an unknown type.
   *
   * <p>This will override previous calls to {@link #dontTransform()} and previous calls to {@link
   * #downsample(DownsampleStrategy)}.
   *
   * @see #transform(Class, Transformation)
   * @see #optionalFitCenter()
   */
  @NonNull
  @CheckResult
  public T fitCenter() {
    return scaleOnlyTransform(DownsampleStrategy.FIT_CENTER, new FitCenter());
  }

  /**
   * Applies {@link com.bumptech.glide.load.resource.bitmap.CenterInside} to all default types,
   * {@link DownsampleStrategy#CENTER_INSIDE} to image types, and ignores unknown types.
   *
   * <p>This will override previous calls to {@link #dontTransform()} and previous calls to {@link
   * #downsample(DownsampleStrategy)}.
   *
   * @see #optionalTransform(Class, Transformation)
   * @see #centerInside()
   */
  @NonNull
  @CheckResult
  public T optionalCenterInside() {
    return optionalScaleOnlyTransform(DownsampleStrategy.CENTER_INSIDE, new CenterInside());
  }

  /**
   * Applies {@link CenterInside} to all default types, {@link DownsampleStrategy#CENTER_INSIDE} to
   * image types and throws an exception if asked to transform an unknown type.
   *
   * <p>This will override previous calls to {@link #dontTransform()} and previous calls to {@link
   * #downsample(DownsampleStrategy)}.
   *
   * @see #transform(Class, Transformation)
   * @see #optionalCenterInside()
   */
  @NonNull
  @CheckResult
  public T centerInside() {
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
  @NonNull
  @CheckResult
  public T optionalCircleCrop() {
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
  @NonNull
  @CheckResult
  public T circleCrop() {
    return transform(DownsampleStrategy.CENTER_INSIDE, new CircleCrop());
  }

  // calling optionalTransform() on the result of clone() requires greater access.
  // calling downsample is guaranteed to modify the current object by the isAutoCloneEnabledCheck.
  @SuppressWarnings({"WeakerAccess", "CheckResult"})
  @NonNull
  final T optionalTransform(
      @NonNull DownsampleStrategy downsampleStrategy,
      @NonNull Transformation<Bitmap> transformation) {
    if (isAutoCloneEnabled) {
      return clone().optionalTransform(downsampleStrategy, transformation);
    }

    downsample(downsampleStrategy);
    return transform(transformation, /* isRequired= */ false);
  }

  // calling transform() on the result of clone() requires greater access.
  // calling downsample is guaranteed to modify the current object by the isAutoCloneEnabledCheck.
  @SuppressWarnings({"WeakerAccess", "CheckResult"})
  @NonNull
  @CheckResult
  final T transform(
      @NonNull DownsampleStrategy downsampleStrategy,
      @NonNull Transformation<Bitmap> transformation) {
    if (isAutoCloneEnabled) {
      return clone().transform(downsampleStrategy, transformation);
    }

    downsample(downsampleStrategy);
    return transform(transformation);
  }

  @NonNull
  private T scaleOnlyTransform(
      @NonNull DownsampleStrategy strategy, @NonNull Transformation<Bitmap> transformation) {
    return scaleOnlyTransform(strategy, transformation, true /*isTransformationRequired*/);
  }

  @NonNull
  private T optionalScaleOnlyTransform(
      @NonNull DownsampleStrategy strategy, @NonNull Transformation<Bitmap> transformation) {
    return scaleOnlyTransform(strategy, transformation, false /*isTransformationRequired*/);
  }

  // We know that result will always be T since we created result.
  @SuppressWarnings("unchecked")
  @NonNull
  private T scaleOnlyTransform(
      @NonNull DownsampleStrategy strategy,
      @NonNull Transformation<Bitmap> transformation,
      boolean isTransformationRequired) {
    BaseRequestOptions<T> result =
        isTransformationRequired
            ? transform(strategy, transformation)
            : optionalTransform(strategy, transformation);
    result.isScaleOnlyOrNoTransform = true;
    return (T) result;
  }

  /**
   * Applies the given {@link Transformation} for {@link Bitmap Bitmaps} to the default types
   * ({@link Bitmap}, {@link android.graphics.drawable.BitmapDrawable}, and {@link
   * com.bumptech.glide.load.resource.gif.GifDrawable}) and throws an exception if asked to
   * transform an unknown type.
   *
   * <p>This will override previous calls to {@link #dontTransform()}.
   *
   * @param transformation Any {@link Transformation} for {@link Bitmap}s.
   * @see #optionalTransform(Transformation)
   * @see #optionalTransform(Class, Transformation)
   */
  // Guaranteed to modify the current object by the isAutoCloneEnabledCheck.
  @SuppressWarnings("CheckResult")
  @NonNull
  @CheckResult
  public T transform(@NonNull Transformation<Bitmap> transformation) {
    return transform(transformation, /* isRequired= */ true);
  }

  /**
   * Applies the given {@link Transformation}s in the given order for {@link Bitmap Bitmaps} to the
   * default types ({@link Bitmap}, {@link android.graphics.drawable.BitmapDrawable}, and {@link
   * com.bumptech.glide.load.resource.gif.GifDrawable}) and throws an exception if asked to
   * transform an unknown type.
   *
   * <p>This will override previous calls to {@link #dontTransform()}.
   *
   * @param transformations One or more {@link Transformation}s for {@link Bitmap}s.
   * @see #optionalTransform(Transformation)
   * @see #optionalTransform(Class, Transformation)
   */
  // Guaranteed to modify the current object by the isAutoCloneEnabledCheck.
  @SuppressWarnings({"unchecked", "varargs", "CheckResult"})
  @NonNull
  @CheckResult
  public T transform(@NonNull Transformation<Bitmap>... transformations) {
    if (transformations.length > 1) {
      return transform(new MultiTransformation<>(transformations), /* isRequired= */ true);
    } else if (transformations.length == 1) {
      return transform(transformations[0]);
    } else {
      return selfOrThrowIfLocked();
    }
  }

  /**
   * Applies the given {@link Transformation}s in the given order for {@link Bitmap Bitmaps} to the
   * default types ({@link Bitmap}, {@link android.graphics.drawable.BitmapDrawable}, and {@link
   * com.bumptech.glide.load.resource.gif.GifDrawable}) and throws an exception if asked to
   * transform an unknown type.
   *
   * <p>This will override previous calls to {@link #dontTransform()}.
   *
   * @deprecated Deprecated due to api update, use {@link #transform(Transformation[])} instead
   * @param transformations One or more {@link Transformation}s for {@link Bitmap}s.
   * @see #optionalTransform(Transformation)
   * @see #optionalTransform(Class, Transformation)
   */
  // Guaranteed to modify the current object by the isAutoCloneEnabledCheck.
  @SuppressWarnings({"unchecked", "varargs", "CheckResult"})
  @NonNull
  @CheckResult
  @Deprecated
  public T transforms(@NonNull Transformation<Bitmap>... transformations) {
    return transform(new MultiTransformation<>(transformations), /* isRequired= */ true);
  }

  /**
   * Applies the given {@link Transformation} for {@link Bitmap Bitmaps} to the default types
   * ({@link Bitmap}, {@link android.graphics.drawable.BitmapDrawable}, and {@link
   * com.bumptech.glide.load.resource.gif.GifDrawable}) and ignores unknown types.
   *
   * <p>This will override previous calls to {@link #dontTransform()}.
   *
   * @param transformation Any {@link Transformation} for {@link Bitmap}s.
   * @see #transform(Transformation)
   * @see #transform(Class, Transformation)
   */
  // Guaranteed to modify the current object by the isAutoCloneEnabledCheck.
  @SuppressWarnings("CheckResult")
  @NonNull
  @CheckResult
  public T optionalTransform(@NonNull Transformation<Bitmap> transformation) {
    return transform(transformation, /* isRequired= */ false);
  }

  @NonNull
  T transform(@NonNull Transformation<Bitmap> transformation, boolean isRequired) {
    if (isAutoCloneEnabled) {
      return clone().transform(transformation, isRequired);
    }

    DrawableTransformation drawableTransformation =
        new DrawableTransformation(transformation, isRequired);
    transform(Bitmap.class, transformation, isRequired);
    transform(Drawable.class, drawableTransformation, isRequired);
    // TODO: remove BitmapDrawable decoder and this transformation.
    // Registering as BitmapDrawable is simply an optimization to avoid some iteration and
    // isAssignableFrom checks when obtaining the transformation later on. It can be removed without
    // affecting the functionality.
    transform(BitmapDrawable.class, drawableTransformation.asBitmapDrawable(), isRequired);
    transform(GifDrawable.class, new GifDrawableTransformation(transformation), isRequired);
    return selfOrThrowIfLocked();
  }

  /**
   * Applies the given {@link Transformation} for any decoded resource of the given type and allows
   * unknown resource types to be ignored.
   *
   * <p>Users can apply different transformations for each resource class. Applying a {@link
   * Transformation} for a resource type that already has a {@link Transformation} will override the
   * previous call.
   *
   * <p>If any calls are made to the non-optional transform methods, then attempting to transform an
   * unknown resource class will throw an exception. To allow unknown types, users must always call
   * the optional version of each method.
   *
   * <p>This will override previous calls to {@link #dontTransform()}.
   *
   * @param resourceClass The type of resource to transform.
   * @param transformation The {@link Transformation} to apply.
   */
  @NonNull
  @CheckResult
  public <Y> T optionalTransform(
      @NonNull Class<Y> resourceClass, @NonNull Transformation<Y> transformation) {
    return transform(resourceClass, transformation, /* isRequired= */ false);
  }

  @NonNull
  <Y> T transform(
      @NonNull Class<Y> resourceClass,
      @NonNull Transformation<Y> transformation,
      boolean isRequired) {
    if (isAutoCloneEnabled) {
      return clone().transform(resourceClass, transformation, isRequired);
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
    if (isRequired) {
      fields |= TRANSFORMATION_REQUIRED;
      isTransformationRequired = true;
    }
    return selfOrThrowIfLocked();
  }

  /**
   * Applies the given {@link Transformation} for any decoded resource of the given type and throws
   * if asked to transform an unknown resource type.
   *
   * <p>This will override previous calls to {@link #dontTransform()}.
   *
   * @param resourceClass The type of resource to transform.
   * @param transformation The {@link Transformation} to apply.
   * @see #optionalTransform(Class, Transformation)
   */
  // Guaranteed to modify the current object by the isAutoCloneEnabledCheck.
  @SuppressWarnings("CheckResult")
  @NonNull
  @CheckResult
  public <Y> T transform(
      @NonNull Class<Y> resourceClass, @NonNull Transformation<Y> transformation) {
    return transform(resourceClass, transformation, /* isRequired= */ true);
  }

  /**
   * Removes all applied {@link Transformation Transformations} for all resource classes and allows
   * unknown resource types to be transformed without throwing an exception.
   */
  @NonNull
  @CheckResult
  public T dontTransform() {
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
   * <p>To disable transitions (fades etc) use {@link
   * com.bumptech.glide.TransitionOptions#dontTransition()}
   */
  // Guaranteed to modify the current object by the isAutoCloneEnabledCheck.
  @SuppressWarnings("CheckResult")
  @NonNull
  @CheckResult
  public T dontAnimate() {
    return set(GifOptions.DISABLE_ANIMATION, true);
  }

  /**
   * Updates this options set with any options that are explicitly set in the given {@code T} object
   * and returns this object if {@link #autoClone()} is disabled or a new {@code T} object if {@link
   * #autoClone()} is enabled.
   *
   * <p>{@code #apply} only replaces those values that are explicitly set in the given {@code T}. If
   * you need to completely reset all previously set options, create a new {@code T} object instead
   * of using this method.
   *
   * <p>The options that will be set to values in the returned {@code T} object is the intersection
   * of the set of options in this {@code T} object and the given {@code T} object that were
   * explicitly set. If the values of any of the options conflict, the values in the returned {@code
   * T} object will be set to those in the given {@code T} object.
   */
  @NonNull
  @CheckResult
  public T apply(@NonNull BaseRequestOptions<?> o) {
    if (isAutoCloneEnabled) {
      return clone().apply(o);
    }
    BaseRequestOptions<?> other = o;

    if (isSet(other.fields, SIZE_MULTIPLIER)) {
      sizeMultiplier = other.sizeMultiplier;
    }
    if (isSet(other.fields, USE_UNLIMITED_SOURCE_GENERATORS_POOL)) {
      useUnlimitedSourceGeneratorsPool = other.useUnlimitedSourceGeneratorsPool;
    }
    if (isSet(other.fields, USE_ANIMATION_POOL)) {
      useAnimationPool = other.useAnimationPool;
    }
    if (isSet(other.fields, DISK_CACHE_STRATEGY)) {
      diskCacheStrategy = other.diskCacheStrategy;
    }
    if (isSet(other.fields, PRIORITY)) {
      priority = other.priority;
    }
    if (isSet(other.fields, ERROR_PLACEHOLDER)) {
      errorPlaceholder = other.errorPlaceholder;
      errorId = 0;
      fields &= ~ERROR_ID;
    }
    if (isSet(other.fields, ERROR_ID)) {
      errorId = other.errorId;
      errorPlaceholder = null;
      fields &= ~ERROR_PLACEHOLDER;
    }
    if (isSet(other.fields, PLACEHOLDER)) {
      placeholderDrawable = other.placeholderDrawable;
      placeholderId = 0;
      fields &= ~PLACEHOLDER_ID;
    }
    if (isSet(other.fields, PLACEHOLDER_ID)) {
      placeholderId = other.placeholderId;
      placeholderDrawable = null;
      fields &= ~PLACEHOLDER;
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
      fallbackId = 0;
      fields &= ~FALLBACK_ID;
    }
    if (isSet(other.fields, FALLBACK_ID)) {
      fallbackId = other.fallbackId;
      fallbackDrawable = null;
      fields &= ~FALLBACK;
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

  /**
   * Returns {@code true} if this {@link BaseRequestOptions} is equivalent to the given
   * {@link BaseRequestOptions} (has all of the same options and sizes).
   *
   * <p>This method is identical to {@link #equals(Object)}, but this can not be overridden. We need
   * to use this method instead of {@link #equals(Object)}, because child classes may have additional
   * fields, such as listeners and models, that should not be considered when checking for equality.
   */
  public final boolean isEquivalentTo(BaseRequestOptions<?> other) {
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

  @Override
  public boolean equals(Object o) {
    if (o instanceof BaseRequestOptions<?>) {
      return isEquivalentTo((BaseRequestOptions<?>) o);
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
   * <p>Once locked, the only way to unlock is to use {@link #clone()}
   */
  @NonNull
  @SuppressWarnings("unchecked")
  public T lock() {
    isLocked = true;
    // This is the only place we should not check locked.
    return self();
  }

  /**
   * Similar to {@link #lock()} except that mutations cause a {@link #clone()} operation to happen
   * before the mutation resulting in all methods returning a new Object and leaving the original
   * locked object unmodified.
   *
   * <p>Auto clone is not retained by cloned objects returned from mutations. The cloned objects are
   * mutable and are not locked.
   */
  @NonNull
  public T autoClone() {
    if (isLocked && !isAutoCloneEnabled) {
      throw new IllegalStateException(
          "You cannot auto lock an already locked options object" + ", try clone() first");
    }
    isAutoCloneEnabled = true;
    return lock();
  }

  @NonNull
  @SuppressWarnings("unchecked")
  protected final T selfOrThrowIfLocked() {
    if (isLocked) {
      throw new IllegalStateException("You cannot modify locked T, consider clone()");
    }
    return self();
  }

  protected final boolean isAutoCloneEnabled() {
    return isAutoCloneEnabled;
  }

  public final boolean isDiskCacheStrategySet() {
    return isSet(DISK_CACHE_STRATEGY);
  }

  public final boolean isSkipMemoryCacheSet() {
    return isSet(IS_CACHEABLE);
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

  @SuppressWarnings("WeakerAccess")
  @Nullable
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

  @SuppressWarnings("WeakerAccess")
  @Nullable
  public final Drawable getPlaceholderDrawable() {
    return placeholderDrawable;
  }

  @SuppressWarnings("WeakerAccess")
  public final int getFallbackId() {
    return fallbackId;
  }

  @SuppressWarnings("WeakerAccess")
  @Nullable
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

  boolean isScaleOnlyOrNoTransform() {
    return isScaleOnlyOrNoTransform;
  }

  private boolean isSet(int flag) {
    return isSet(fields, flag);
  }

  // get is just as clear.
  @SuppressWarnings("PMD.BooleanGetMethodName")
  public final boolean getUseUnlimitedSourceGeneratorsPool() {
    return useUnlimitedSourceGeneratorsPool;
  }

  // get is just as clear.
  @SuppressWarnings("PMD.BooleanGetMethodName")
  public final boolean getUseAnimationPool() {
    return useAnimationPool;
  }

  // get is just as clear.
  @SuppressWarnings("PMD.BooleanGetMethodName")
  public final boolean getOnlyRetrieveFromCache() {
    return onlyRetrieveFromCache;
  }

  @SuppressWarnings("unchecked")
  private T self() {
    return (T) this;
  }
}
