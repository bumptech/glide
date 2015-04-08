package com.bumptech.glide.request;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.UnitTransformation;
import com.bumptech.glide.load.resource.bitmap.BitmapDrawableTransformation;
import com.bumptech.glide.load.resource.bitmap.BitmapEncoder;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
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
 * Contains and exposes a variety of non type specific options that can be applied to a load in
 * Glide.
 *
 * <p> If {@link #lock()} has been called, this class will throw if any further mutations are
 * attempted. To unlock, use {@link #clone()}. </p>
 *
 * @param <CHILD> The concrete and <em>final</em> subclass.
 */
public abstract class BaseRequestOptions<CHILD extends BaseRequestOptions<CHILD>>
    implements Cloneable {
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
  private static final int TAG = 1 << 11;
  private static final int TRANSFORMATION = 1 << 12;
  private static final int RESOURCE_CLASS = 1 << 13;

  private int fields;

  private float sizeMultiplier = 1f;
  private DiskCacheStrategy diskCacheStrategy = DiskCacheStrategy.AUTOMATIC;
  private Priority priority = Priority.NORMAL;
  private Drawable errorPlaceholder;
  private int errorId;
  private Drawable placeholderDrawable;
  private int placeholderId;
  private boolean isCacheable = true;
  private int overrideHeight = UNSET;
  private int overrideWidth = UNSET;
  private Key signature = EmptySignature.obtain();
  private String tag;
  private boolean isTransformationRequired;

  private Options options = new Options();
  private Map<Class<?>, Transformation<?>> transformations = new HashMap<>();
  private Class<?> resourceClass = Object.class;
  private boolean isLocked;

  public final CHILD tag(String tag) {
    this.tag = tag;
    fields |= TAG;
    return selfOrThrowIfLocked();
  }

  /**
   * Applies a multiplier to the {@link com.bumptech.glide.request.target.Target}'s size before
   * loading the resource. Useful for loading thumbnails or trying to avoid loading huge resources
   * (particularly {@link android.graphics.Bitmap}s on devices with overly dense screens.
   *
   * @param sizeMultiplier The multiplier to apply to the
   *                       {@link com.bumptech.glide.request.target.Target}'s dimensions when
   *                       loading the resource.
   * @return This request builder.
   */
  public final CHILD sizeMultiplier(float sizeMultiplier) {
    if (sizeMultiplier < 0f || sizeMultiplier > 1f) {
      throw new IllegalArgumentException("sizeMultiplier must be between 0 and 1");
    }
    this.sizeMultiplier = sizeMultiplier;
    fields |= SIZE_MULTIPLIER;

    return selfOrThrowIfLocked();
  }

  /**
   * Sets the {@link com.bumptech.glide.load.engine.DiskCacheStrategy} to use for this load.
   *
   * <p> Defaults to {@link com.bumptech.glide.load.engine.DiskCacheStrategy#AUTOMATIC}. </p>
   *
   * <p> For most applications {@link com.bumptech.glide.load.engine.DiskCacheStrategy#RESOURCE} is
   * ideal. Applications that use the same resource multiple times in multiple sizes and are willing
   * to trade off some speed and disk space in return for lower bandwidth usage may want to consider
   * using {@link com.bumptech.glide.load.engine.DiskCacheStrategy#DATA} or
   * {@link com.bumptech.glide.load.engine.DiskCacheStrategy#ALL}. </p>
   *
   * @param strategy The strategy to use.
   * @return This request builder.
   */
  public final CHILD diskCacheStrategy(DiskCacheStrategy strategy) {
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
  public final CHILD priority(Priority priority) {
    this.priority = Preconditions.checkNotNull(priority);
    fields |= PRIORITY;

    return selfOrThrowIfLocked();
  }

  /**
   * Sets an {@link android.graphics.drawable.Drawable} to display while a resource is loading.
   *
   * @param drawable The drawable to display as a placeholder.
   * @return This request builder.
   */
  public final CHILD placeholder(Drawable drawable) {
    this.placeholderDrawable = drawable;
    fields |= PLACEHOLDER;

    return selfOrThrowIfLocked();
  }

  /**
   * Sets an Android resource id for a {@link android.graphics.drawable.Drawable} resource to
   * display while a resource is loading.
   *
   * @param resourceId The id of the resource to use as a placeholder
   * @return This request builder.
   */
  public final CHILD placeholder(int resourceId) {
    this.placeholderId = resourceId;
    fields |= PLACEHOLDER_ID;

    return selfOrThrowIfLocked();
  }

  /**
   * Sets a {@link Drawable} to display if a load fails.
   *
   * @param drawable The drawable to display.
   * @return This request builder.
   */
  public final CHILD error(Drawable drawable) {
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
  public final CHILD error(int resourceId) {
    this.errorId = resourceId;
    fields |= ERROR_ID;

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
  public final CHILD skipMemoryCache(boolean skip) {
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
  public final CHILD override(int width, int height) {
    this.overrideWidth = width;
    this.overrideHeight = height;
    fields |= OVERRIDE;

    return selfOrThrowIfLocked();
  }

  /**
   * Sets some additional data to be mixed in to the memory and disk cache keys allowing the caller
   * more control over when cached data is invalidated.
   *
   * <p> Note - The signature does not replace the cache key, it is purely additive. </p>
   *
   * @param signature A unique non-null {@link com.bumptech.glide.load.Key} representing the current
   *                  state of the model that will be mixed in to the cache key.
   * @return This request builder.
   * @see com.bumptech.glide.signature.StringSignature
   */
  public final CHILD signature(Key signature) {
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
  public final CHILD clone() {
    try {
      BaseRequestOptions<CHILD> result = (BaseRequestOptions<CHILD>) super.clone();
      result.options = new Options();
      result.options.putAll(options);
      result.transformations = new HashMap<>();
      result.transformations.putAll(transformations);
      result.isLocked = false;
      return (CHILD) result;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  public final <T> CHILD set(Option<T> option, T value) {
    options.set(option, value);
    return selfOrThrowIfLocked();
  }

  public final CHILD decode(Class<?> resourceClass) {
    this.resourceClass = Preconditions.checkNotNull(resourceClass);
    fields |= RESOURCE_CLASS;
    return selfOrThrowIfLocked();
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
  public CHILD encodeFormat(Bitmap.CompressFormat format) {
    return set(BitmapEncoder.COMPRESSION_FORMAT, format);
  }

  /**
   * Sets the value for key
   * {@link com.bumptech.glide.load.resource.bitmap.BitmapEncoder#COMPRESSION_QUALITY}.
   */
  public CHILD encodeQuality(Integer quality) {
    return set(BitmapEncoder.COMPRESSION_QUALITY, quality);
  }

  public CHILD format(DecodeFormat format) {
    return set(Downsampler.DECODE_FORMAT, Preconditions.checkNotNull(format));
  }

  public CHILD frame(long frame) {
    return set(VideoBitmapDecoder.TARGET_FRAME, frame);
  }

  public CHILD downsample(DownsampleStrategy strategy) {
    return set(Downsampler.DOWNSAMPLE_STRATEGY, strategy);
  }

  /**
   * Applies {@link com.bumptech.glide.load.resource.bitmap.CenterCrop} to all default types, and
   * ignores unknown types.
   *
   * @param context Any {@link android.content.Context}.
   * @see #optionalTransform(Class, com.bumptech.glide.load.Transformation)
   * @see #centerCrop(android.content.Context)
   */
  public CHILD optionalCenterCrop(Context context) {
    downsample(DownsampleStrategy.CENTER_OUTSIDE);
    return optionalTransform(context, new CenterCrop(context));
  }

  /**
   * Applies {@link com.bumptech.glide.load.resource.bitmap.CenterCrop} to all default types and
   * throws an exception if asked to transform an unknown type.
   *
   * @param context Any {@link android.content.Context}.
   * @see #transform(Class, com.bumptech.glide.load.Transformation)
   * @see #optionalCenterCrop(android.content.Context)
   */
  public CHILD centerCrop(Context context) {
    downsample(DownsampleStrategy.CENTER_OUTSIDE);
    return transform(context, new CenterCrop(context));
  }

  /**
   * Applies {@link com.bumptech.glide.load.resource.bitmap.FitCenter} to all default types, and
   * ignores unknown types.
   *
   * @param context Any {@link android.content.Context}.
   * @see #optionalTransform(Class, com.bumptech.glide.load.Transformation)
   * @see #fitCenter(android.content.Context)
   */
  public CHILD optionalFitCenter(Context context) {
    downsample(DownsampleStrategy.CENTER_INSIDE);
    return optionalTransform(context, new FitCenter(context));
  }

  /**
   * Applies {@link com.bumptech.glide.load.resource.bitmap.FitCenter} to all default types and
   * throws an exception if asked to transform an unknown type.
   *
   * @param context Any {@link android.content.Context}.
   * @see #transform(Class, com.bumptech.glide.load.Transformation)
   * @see #optionalFitCenter(android.content.Context)
   */
  public CHILD fitCenter(Context context) {
    downsample(DownsampleStrategy.CENTER_INSIDE);
    return transform(context, new FitCenter(context));
  }

  /**
   * Applies the given {@link com.bumptech.glide.load.Transformation} for
   * {@link android.graphics.Bitmap Bitmaps} to the default types ({@link android.graphics.Bitmap},
   * {@link android.graphics.drawable.BitmapDrawable}, and
   * {@link com.bumptech.glide.load.resource.gif.GifDrawable})
   * and throws an exception if asked to transform an unknown type.
   *
   * @param context        Any {@link android.content.Context}.
   * @param transformation Any {@link com.bumptech.glide.load.Transformation} for
   *                       {@link android.graphics.Bitmap}s.
   * @see #optionalTransform(android.content.Context, com.bumptech.glide.load.Transformation)
   * @see #optionalTransform(Class, com.bumptech.glide.load.Transformation)
   */
  public CHILD transform(Context context, Transformation<Bitmap> transformation) {
    optionalTransform(context, transformation);
    isTransformationRequired = true;
    return selfOrThrowIfLocked();
  }

  /**
   * Applies the given {@link com.bumptech.glide.load.Transformation} for
   * {@link android.graphics.Bitmap Bitmaps} to the default types ({@link android.graphics.Bitmap},
   * {@link android.graphics.drawable.BitmapDrawable}, and
   * {@link com.bumptech.glide.load.resource.gif.GifDrawable}) and ignores unknown types.
   *
   * @param context        Any {@link android.content.Context}.
   * @param transformation Any {@link com.bumptech.glide.load.Transformation} for
   *                       {@link android.graphics.Bitmap}s.
   * @see #transform(android.content.Context, com.bumptech.glide.load.Transformation)
   * @see #transform(Class, com.bumptech.glide.load.Transformation)
   */
  public CHILD optionalTransform(Context context, Transformation<Bitmap> transformation) {
    optionalTransform(Bitmap.class, transformation);
    // TODO: remove BitmapDrawable decoder and this transformation.
    optionalTransform(BitmapDrawable.class,
        new BitmapDrawableTransformation(context, transformation));
    optionalTransform(GifDrawable.class, new GifDrawableTransformation(context, transformation));
    return selfOrThrowIfLocked();
  }

  /**
   * Applies the given {@link com.bumptech.glide.load.Transformation} for any decoded resource of
   * the given type and allows unknown resource types to be ignored.
   *
   * <p> Users can apply different transformations for each resource class. Applying a
   * {@link com.bumptech.glide.load.Transformation} for a resource type that already has a
   * {@link com.bumptech.glide.load.Transformation} will override the previous call. </p>
   *
   * <p> If any calls are made to the non-optional transform methods, then attempting to transform
   * an unknown resource class will throw an exception. To allow unknown types, users must always
   * call the optional version of each method. </p>
   *
   * @param resourceClass  The type of resource to transform.
   * @param transformation The {@link com.bumptech.glide.load.Transformation} to apply.
   */
  public final <T> CHILD optionalTransform(Class<T> resourceClass,
      Transformation<T> transformation) {
    Preconditions.checkNotNull(resourceClass);
    Preconditions.checkNotNull(transformation);
    fields |= TRANSFORMATION;
    transformations.put(resourceClass, transformation);
    return selfOrThrowIfLocked();
  }

  /**
   * Applies the given {@link com.bumptech.glide.load.Transformation} for any decoded resource of
   * the given type and throws if asked to transform an unknown resource type.
   *
   * @param resourceClass  The type of resource to transform.
   * @param transformation The {@link com.bumptech.glide.load.Transformation} to apply.
   * @see #optionalTransform(Class, com.bumptech.glide.load.Transformation)
   */
  public final <T> CHILD transform(Class<T> resourceClass, Transformation<T> transformation) {
    optionalTransform(resourceClass, transformation);
    isTransformationRequired = true;
    return selfOrThrowIfLocked();
  }

  /**
   * Removes all applied {@link com.bumptech.glide.load.Transformation Transformations} for all
   * resource classes and allows unknown resource types to be transformed without throwing an
   * exception.
   */
  public final CHILD dontTransform() {
    fields &= ~TRANSFORMATION;
    transformations.clear();
    isTransformationRequired = false;
    return selfOrThrowIfLocked();
  }

  /**
   * Disables resource decoders that return animated resources so any resource returned will be
   * static.
   *
   * <p> To disable transitions (fades etc) use
   * {@link com.bumptech.glide.TransitionOptions#dontTransition()}</p>
   */
  public final CHILD dontAnimate() {
    set(ByteBufferGifDecoder.DISABLE_ANIMATION, true);
    set(StreamGifDecoder.DISABLE_ANIMATION, true);
    return selfOrThrowIfLocked();
  }

  public final Map<Class<?>, Transformation<?>> getTransformations() {
    return transformations;
  }

  @SuppressWarnings("unchecked")
  public final <T> Transformation<T> getTransformation(Class<T> resourceClass) {
    Transformation<T> result = (Transformation<T>) transformations.get(resourceClass);
    if (result == null) {
      if (!transformations.isEmpty() && isTransformationRequired) {
        throw new IllegalArgumentException(
            "Missing transformation for " + resourceClass + ". If you wish to"
                + " ignore unknown resource types, use the optional transformation methods.");
      } else {
        return UnitTransformation.get();
      }
    }
    return result;
  }

  /**
   * Throws if any further mutations are attempted.
   *
   * <p> Once locked, the only way to unlock is to use {@link #clone()} </p>
   */
  @SuppressWarnings("unchecked")
  public final CHILD lock() {
    isLocked = true;
    // This is the only place we should not check locked.
    return (CHILD) this;
  }

  public final Options getOptions() {
    return options;
  }

  public final Class<?> getResourceClass() {
    return resourceClass;
  }

  public final CHILD apply(BaseRequestOptions<?> other) {
    if (isSet(other.fields, DISK_CACHE_STRATEGY)) {
      diskCacheStrategy = other.diskCacheStrategy;
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
    if (isSet(other.fields, SIGNATURE)) {
      signature = other.signature;
    }
    if (isSet(other.fields, PRIORITY)) {
      priority = other.priority;
    }
    if (isSet(other.fields, OVERRIDE)) {
      overrideWidth = other.overrideWidth;
      overrideHeight = other.overrideHeight;
    }
    if (isSet(other.fields, SIZE_MULTIPLIER)) {
      sizeMultiplier = other.sizeMultiplier;
    }
    if (isSet(other.fields, TAG)) {
      tag = other.tag;
    }
    if (isSet(other.fields, RESOURCE_CLASS)) {
      resourceClass = other.resourceClass;
    }

    isTransformationRequired |= other.isTransformationRequired;
    fields |= other.fields;
    transformations.putAll(other.transformations);
    options.putAll(other.options);

    return selfOrThrowIfLocked();
  }

  public final DiskCacheStrategy getDiskCacheStrategy() {
    return diskCacheStrategy;
  }

  public final Drawable getErrorPlaceholder() {
    return errorPlaceholder;
  }

  public final int getErrorId() {
    return errorId;
  }

  public final int getPlaceholderId() {
    return placeholderId;
  }

  public final Drawable getPlaceholderDrawable() {
    return placeholderDrawable;
  }

  public final boolean isMemoryCacheable() {
    return isCacheable;
  }

  public final Key getSignature() {
    return signature;
  }

  public final boolean isPrioritySet() {
    return isSet(PRIORITY);
  }

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

  public final String getTag() {
    return tag;
  }

  @SuppressWarnings("unchecked")
  private CHILD selfOrThrowIfLocked() {
    if (isLocked) {
      throw new IllegalStateException("You cannot modify locked RequestOptions, consider clone()");
    }
    return (CHILD) this;
  }

  private boolean isSet(int flag) {
    return isSet(fields, flag);
  }

  private static boolean isSet(int fields, int flag) {
    return (fields & flag) != 0;
  }
}
