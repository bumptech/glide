package com.bumptech.glide.load.resource.bitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder.UseResourceClassForDecoding;
import com.bumptech.glide.GlideContext;
import com.bumptech.glide.GlideExperiments;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import java.security.MessageDigest;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Applies a {@link Bitmap} {@link Transformation} to {@link Drawable}s by first attempting to
 * convert the {@link Drawable} to a {@link Bitmap} and then running the {@link Transformation} on
 * the converted {@link Bitmap}.
 *
 * <p>This class is relatively efficient for {@link BitmapDrawable} where the {@link Bitmap} is
 * readily accessible. For non-{@link Bitmap} based {@link Drawable}s, this class must first try to
 * draw the {@link Drawable} to a {@link Bitmap} using {@link android.graphics.Canvas}, which is
 * less efficient. Required transformations on {@link Drawable}s that implement {@link Animatable}
 * fail with an exception unless {@link Drawable#getCurrent()} returns a {@link BitmapDrawable} or a
 * non-{@link Animatable} {@link Drawable}; when {@link UseResourceClassForDecoding} is enabled,
 * optional transformations skip non-{@link BitmapDrawable} {@link Animatable} {@link Drawable}
 * classes that do not override {@link Drawable#getCurrent()} without calling {@link
 * Resource#get()}. {@link Drawable}s that return {@code <= 0} for {@link
 * Drawable#getIntrinsicHeight()} and/or {@link Drawable#getIntrinsicWidth()} will fail with an
 * exception if the requested size is {@link
 * com.bumptech.glide.request.target.Target#SIZE_ORIGINAL}. {@link Drawable}s without intrinsic
 * dimensions are drawn using the dimensions provided in {@link Transformation#transform(Context,
 * Resource, int, int)}. As a result, they may be transformed incorrectly or in unexpected ways.
 */
public class DrawableTransformation implements Transformation<Drawable> {

  // Bounded non-evicting map; apps load only a handful of Animatable Drawable classes.
  private static final int MAX_CACHED_CLASSES = 50;
  private static final ConcurrentMap<Class<?>, Boolean> OVERRIDES_GET_CURRENT_CACHE =
      new ConcurrentHashMap<>();

  private final Transformation<Bitmap> wrapped;
  private final boolean isRequired;

  public DrawableTransformation(Transformation<Bitmap> wrapped, boolean isRequired) {
    this.wrapped = wrapped;
    this.isRequired = isRequired;
  }

  @SuppressWarnings("unchecked")
  public Transformation<BitmapDrawable> asBitmapDrawable() {
    return (Transformation<BitmapDrawable>) (Transformation<?>) this;
  }

  @NonNull
  @Override
  public Resource<Drawable> transform(
      @NonNull Context context, @NonNull Resource<Drawable> resource, int outWidth, int outHeight) {
    // Non-container/non-wrapper Animatable drawables that do not override getCurrent() cannot be
    // converted to a Bitmap by DrawableToBitmapConverter, so an optional transformation always
    // returns the original resource unchanged. Check the declared class before calling get() so
    // resources that construct a new drawable per get() call do not allocate an unused instance.
    if (!isRequired
        && isUseResourceClassEnabled(context)
        && isDirectNonBitmapAnimatable(resource.getResourceClass())) {
      return resource;
    }
    BitmapPool bitmapPool = Glide.get(context).getBitmapPool();
    Drawable drawable = resource.get();
    Resource<Bitmap> bitmapResourceToTransform =
        DrawableToBitmapConverter.convert(bitmapPool, drawable, outWidth, outHeight);
    if (bitmapResourceToTransform == null) {
      if (isRequired) {
        throw new IllegalArgumentException("Unable to convert " + drawable + " to a Bitmap");
      } else {
        return resource;
      }
    }
    Resource<Bitmap> transformedBitmapResource =
        wrapped.transform(context, bitmapResourceToTransform, outWidth, outHeight);

    if (transformedBitmapResource.equals(bitmapResourceToTransform)) {
      transformedBitmapResource.recycle();
      return resource;
    } else {
      return newDrawableResource(context, transformedBitmapResource);
    }
  }

  // It's clearer to cast the result in a separate line from obtaining it.
  @SuppressWarnings({"unchecked", "PMD.UnnecessaryLocalBeforeReturn"})
  private Resource<Drawable> newDrawableResource(Context context, Resource<Bitmap> transformed) {
    Resource<? extends Drawable> result =
        LazyBitmapDrawableResource.obtain(context.getResources(), transformed);
    return (Resource<Drawable>) result;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof DrawableTransformation) {
      DrawableTransformation other = (DrawableTransformation) o;
      return wrapped.equals(other.wrapped);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return wrapped.hashCode();
  }

  @Override
  public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
    wrapped.updateDiskCacheKey(messageDigest);
  }

  private static boolean isUseResourceClassEnabled(Context context) {
    // DecodeJob passes GlideContext into Transformation.transform(); non-GlideContext callers
    // fall back to calling resource.get().
    if (!(context instanceof GlideContext)) {
      return false;
    }
    GlideExperiments experiments = ((GlideContext) context).getExperiments();
    return experiments != null && experiments.isEnabled(UseResourceClassForDecoding.class);
  }

  private static boolean isDirectNonBitmapAnimatable(@Nullable Class<?> drawableClass) {
    // Require a Drawable subtype so raw interface tokens (e.g. Animatable.class) fall back to
    // get().
    return drawableClass != null
        && Drawable.class.isAssignableFrom(drawableClass)
        && Animatable.class.isAssignableFrom(drawableClass)
        && !BitmapDrawable.class.isAssignableFrom(drawableClass)
        && !overridesGetCurrent(drawableClass);
  }

  private static boolean overridesGetCurrent(Class<?> drawableClass) {
    // Avoid computeIfAbsent for minSdk 23 compatibility (added in API 24).
    Boolean cached = OVERRIDES_GET_CURRENT_CACHE.get(drawableClass);
    if (cached != null) {
      return cached;
    }
    boolean overrides;
    try {
      // Drawable.getCurrent() is public; any subclass declaring it has a different declaring class.
      overrides = !Drawable.class.equals(drawableClass.getMethod("getCurrent").getDeclaringClass());
    } catch (NoSuchMethodException | SecurityException e) {
      // Conservatively fall back to resource.get() if reflection fails.
      return true;
    }
    if (OVERRIDES_GET_CURRENT_CACHE.size() < MAX_CACHED_CLASSES) {
      // Benign race: result is pure per Class<?> and the size cap is soft.
      OVERRIDES_GET_CURRENT_CACHE.put(drawableClass, overrides);
    }
    return overrides;
  }
}
