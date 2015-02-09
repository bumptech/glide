package com.bumptech.glide.request;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.UnitTransformation;
import com.bumptech.glide.load.resource.bitmap.BitmapDrawableTransformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy;
import com.bumptech.glide.load.resource.bitmap.Downsampler;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.bitmap.StreamBitmapDecoder;
import com.bumptech.glide.load.resource.bitmap.VideoBitmapDecoder;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.load.resource.gif.GifDrawableTransformation;
import com.bumptech.glide.signature.EmptySignature;
import com.bumptech.glide.util.Preconditions;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains and exposes a variety of non type specific options that can be applied to a load in Glide.
 *
 * @param <CHILD> The concrete <em>final</em> subclass.
 */
public abstract class BaseRequestOptions<CHILD extends BaseRequestOptions<CHILD>> implements Cloneable {
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
    private DiskCacheStrategy diskCacheStrategy = DiskCacheStrategy.RESULT;
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


    private Map<String, Object> options = new HashMap<>();
    private Map<Class<?>, Transformation<?>> transformations = new HashMap<>();
    private Class<?> resourceClass = Object.class;

    public final CHILD tag(String tag) {
        this.tag = tag;
        fields |= TAG;
        return self();
    }

    /**
     * Applies a multiplier to the {@link com.bumptech.glide.request.target.Target}'s size before loading the resource.
     * Useful for loading thumbnails or trying to avoid loading huge resources (particularly
     * {@link android.graphics.Bitmap}s on devices with overly dense screens.
     *
     * @param sizeMultiplier The multiplier to apply to the {@link com.bumptech.glide.request.target.Target}'s
     *                       dimensions when loading the resource.
     * @return This request builder.
     */
    public final CHILD sizeMultiplier(float sizeMultiplier) {
        if (sizeMultiplier < 0f || sizeMultiplier > 1f) {
            throw new IllegalArgumentException("sizeMultiplier must be between 0 and 1");
        }
        this.sizeMultiplier = sizeMultiplier;
        fields |= SIZE_MULTIPLIER;

        return self();
    }

    /**
     * Sets the {@link com.bumptech.glide.load.engine.DiskCacheStrategy} to use for this load. Defaults to
     * {@link com.bumptech.glide.load.engine.DiskCacheStrategy#RESULT}.
     *
     * <p>
     *     For most applications {@link com.bumptech.glide.load.engine.DiskCacheStrategy#RESULT} is ideal.
     *     Applications that use the same resource multiple times in multiple sizes and are willing to trade off some
     *     speed and disk space in return for lower bandwidth usage may want to consider using
     *     {@link com.bumptech.glide.load.engine.DiskCacheStrategy#SOURCE} or
     *     {@link com.bumptech.glide.load.engine.DiskCacheStrategy#RESULT}. Any download only operations should
     *     typically use {@link com.bumptech.glide.load.engine.DiskCacheStrategy#SOURCE}.
     * </p>
     *
     * @param strategy The strategy to use.
     * @return This request builder.
     */
    public final CHILD diskCacheStrategy(DiskCacheStrategy strategy) {
        this.diskCacheStrategy = Preconditions.checkNotNull(strategy);
        fields |= DISK_CACHE_STRATEGY;

        return self();
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

        return self();
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

        return self();
    }

    /**
     * Sets an Android resource id for a {@link android.graphics.drawable.Drawable} resourceto display while a resource
     * is loading.
     *
     * @param resourceId The id of the resource to use as a placeholder
     * @return This request builder.
     */
    public final CHILD placeholder(int resourceId) {
        this.placeholderId = resourceId;
        fields |= PLACEHOLDER_ID;

        return self();
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

        return self();
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

        return self();
    }

    /**
     * Allows the loaded resource to skip the memory cache.
     *
     * <p>
     *     Note - this is not a guarantee. If a request is already pending for this resource and that request is not
     *     also skipping the memory cache, the resource will be cached in memory.
     * </p>
     *
     * @param skip True to allow the resource to skip the memory cache.
     * @return This request builder.
     */
    public final CHILD skipMemoryCache(boolean skip) {
        this.isCacheable = !skip;
        fields |= IS_CACHEABLE;

        return self();
    }

    /**
     * Overrides the {@link com.bumptech.glide.request.target.Target}'s width and height with the given values. This is
     * useful for thumbnails, and should only be used for other cases when you need a very specific image size.
     *
     * @param width The width in pixels to use to load the resource.
     * @param height The height in pixels to use to load the resource.
     * @return This request builder.
     */
    public final CHILD override(int width, int height) {
        this.overrideWidth = width;
        this.overrideHeight = height;
        fields |= OVERRIDE;

        return self();
    }

    /**
     * Sets some additional data to be mixed in to the memory and disk cache keys allowing the caller more control over
     * when cached data is invalidated.
     *
     * <p>
     *     Note - The signature does not replace the cache key, it is purely additive.
     * </p>
     *
     * @see com.bumptech.glide.signature.StringSignature
     *
     * @param signature A unique non-null {@link com.bumptech.glide.load.Key} representing the current state of the
     *                  model that will be mixed in to the cache key.
     * @return This request builder.
     */
    public final CHILD signature(Key signature) {
        this.signature = Preconditions.checkNotNull(signature);
        fields |= SIGNATURE;
        return self();
    }

    /**
     * Returns a copy of this request builder with all of the options set so far on this builder.
     *
     * <p>
     *     This method returns a "deep" copy in that all non-immutable arguments are copied such that changes to one
     *     builder will not affect the other builder. However, in addition to immutable arguments, the current model
     *     is not copied copied so changes to the model will affect both builders.
     * </p>
     */
    @SuppressWarnings("unchecked")
    @Override
    public final CHILD clone() {
        try {
            BaseRequestOptions<CHILD> result = (BaseRequestOptions<CHILD>) super.clone();
            result.options = new HashMap<>();
            result.options.putAll(options);
            result.transformations =  new HashMap<>();
            result.transformations.putAll(transformations);
            return (CHILD) result;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }


    public final <T> CHILD transform(Class<T> resourceClass, Transformation<T> transformation) {
        Preconditions.checkNotNull(resourceClass);
        Preconditions.checkNotNull(transformation);
        fields |= TRANSFORMATION;
        transformations.put(resourceClass, transformation);
        return self();
    }

    public final CHILD dontTransform() {
        fields &= ~TRANSFORMATION;
        transformations.clear();
        return self();
    }

    public final CHILD set(String key, Object option) {
        options.put(key, option);
        return self();
    }

    public final CHILD decode(Class<?> resourceClass) {
        this.resourceClass = Preconditions.checkNotNull(resourceClass);
        fields |= RESOURCE_CLASS;
        return self();
    }

    public final boolean isTransformationSet() {
        return isSet(TRANSFORMATION);
    }

    public CHILD format(DecodeFormat format) {
        return set(Downsampler.KEY_DECODE_FORMAT, Preconditions.checkNotNull(format));
    }

    public CHILD frame(int frame) {
        return set(VideoBitmapDecoder.KEY_TARGET_FRAME, frame);
    }

    public CHILD downsample(DownsampleStrategy strategy) {
        return set(StreamBitmapDecoder.KEY_DOWNSAMPLE_STRATEGY, strategy);
    }

    public CHILD centerCrop(Context context) {
        return transform(context, new CenterCrop(context));
    }

    public CHILD fitCenter(Context context) {
        return transform(context, new FitCenter(context));
    }

    public CHILD transform(Context context, Transformation<Bitmap> transformation) {
        transform(Bitmap.class, transformation);
        // TODO: remove BitmapDrawable decoder and this transformation.
        transform(BitmapDrawable.class, new BitmapDrawableTransformation(context, transformation));
        transform(GifDrawable.class, new GifDrawableTransformation(context, transformation));
        return self();
    }

    @SuppressWarnings("unchecked")
    public final <T> Transformation<T> getTransformation(Class<T> resourceClass) {
        Transformation<T> result = (Transformation<T>) transformations.get(resourceClass);
        if (result == null) {
            if (!transformations.isEmpty()) {
                throw new IllegalArgumentException("Missing transformation for " + resourceClass);
            } else {
                return UnitTransformation.get();
            }
        }
        return result;
    }

    final Map<String, Object> getOptions() {
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

        transformations.putAll(other.transformations);
        options.putAll(other.options);

        return self();
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
    private CHILD self() {
        return (CHILD) this;
    }

    private boolean isSet(int flag) {
        return isSet(fields, flag);
    }

    private static boolean isSet(int fields, int flag) {
        return (fields & flag) != 0;
    }
}
