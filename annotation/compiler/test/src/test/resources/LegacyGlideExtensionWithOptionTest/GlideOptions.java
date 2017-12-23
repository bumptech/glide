package com.bumptech.glide.test;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.CheckResult;
import android.support.annotation.DrawableRes;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy;
import com.bumptech.glide.request.RequestOptions;
import java.lang.Class;
import java.lang.Cloneable;
import java.lang.Override;
import java.lang.SafeVarargs;
import java.lang.SuppressWarnings;

/**
 * Automatically generated from {@link com.bumptech.glide.annotation.GlideExtension} annotated classes.
 *
 * @see RequestOptions
 * @see ExtensionWithOption
 */
@SuppressWarnings("deprecation")
public final class GlideOptions extends RequestOptions implements Cloneable {
  private static GlideOptions fitCenterTransform0;

  private static GlideOptions centerInsideTransform1;

  private static GlideOptions centerCropTransform2;

  private static GlideOptions circleCropTransform3;

  private static GlideOptions noTransformation4;

  private static GlideOptions noAnimation5;

  /**
   * @see RequestOptions#sizeMultiplierOf(float)
   */
  @CheckResult
  public static GlideOptions sizeMultiplierOf(@FloatRange(from = 0.0, to = 1.0) float arg0) {
    return new GlideOptions().sizeMultiplier(arg0);
  }

  /**
   * @see RequestOptions#diskCacheStrategyOf(DiskCacheStrategy)
   */
  @CheckResult
  public static GlideOptions diskCacheStrategyOf(@NonNull DiskCacheStrategy arg0) {
    return new GlideOptions().diskCacheStrategy(arg0);
  }

  /**
   * @see RequestOptions#priorityOf(Priority)
   */
  @CheckResult
  public static GlideOptions priorityOf(@NonNull Priority arg0) {
    return new GlideOptions().priority(arg0);
  }

  /**
   * @see RequestOptions#placeholderOf(Drawable)
   */
  @CheckResult
  public static GlideOptions placeholderOf(@Nullable Drawable arg0) {
    return new GlideOptions().placeholder(arg0);
  }

  /**
   * @see RequestOptions#placeholderOf(int)
   */
  @CheckResult
  public static GlideOptions placeholderOf(@DrawableRes int arg0) {
    return new GlideOptions().placeholder(arg0);
  }

  /**
   * @see RequestOptions#errorOf(Drawable)
   */
  @CheckResult
  public static GlideOptions errorOf(@Nullable Drawable arg0) {
    return new GlideOptions().error(arg0);
  }

  /**
   * @see RequestOptions#errorOf(int)
   */
  @CheckResult
  public static GlideOptions errorOf(@DrawableRes int arg0) {
    return new GlideOptions().error(arg0);
  }

  /**
   * @see RequestOptions#skipMemoryCacheOf(boolean)
   */
  @CheckResult
  public static GlideOptions skipMemoryCacheOf(boolean skipMemoryCache) {
    return new GlideOptions().skipMemoryCache(skipMemoryCache);
  }

  /**
   * @see RequestOptions#overrideOf(int, int)
   */
  @CheckResult
  public static GlideOptions overrideOf(@IntRange(from = 0) int arg0,
      @IntRange(from = 0) int arg1) {
    return new GlideOptions().override(arg0, arg1);
  }

  /**
   * @see RequestOptions#overrideOf(int)
   */
  @CheckResult
  public static GlideOptions overrideOf(@IntRange(from = 0) int arg0) {
    return new GlideOptions().override(arg0);
  }

  /**
   * @see RequestOptions#signatureOf(Key)
   */
  @CheckResult
  public static GlideOptions signatureOf(@NonNull Key arg0) {
    return new GlideOptions().signature(arg0);
  }

  /**
   * @see RequestOptions#fitCenterTransform()
   */
  @CheckResult
  public static GlideOptions fitCenterTransform() {
    if (GlideOptions.fitCenterTransform0 == null) {
      GlideOptions.fitCenterTransform0 =
          new GlideOptions().fitCenter().autoClone();
    }
    return GlideOptions.fitCenterTransform0;
  }

  /**
   * @see RequestOptions#centerInsideTransform()
   */
  @CheckResult
  public static GlideOptions centerInsideTransform() {
    if (GlideOptions.centerInsideTransform1 == null) {
      GlideOptions.centerInsideTransform1 =
          new GlideOptions().centerInside().autoClone();
    }
    return GlideOptions.centerInsideTransform1;
  }

  /**
   * @see RequestOptions#centerCropTransform()
   */
  @CheckResult
  public static GlideOptions centerCropTransform() {
    if (GlideOptions.centerCropTransform2 == null) {
      GlideOptions.centerCropTransform2 =
          new GlideOptions().centerCrop().autoClone();
    }
    return GlideOptions.centerCropTransform2;
  }

  /**
   * @see RequestOptions#circleCropTransform()
   */
  @CheckResult
  public static GlideOptions circleCropTransform() {
    if (GlideOptions.circleCropTransform3 == null) {
      GlideOptions.circleCropTransform3 =
          new GlideOptions().circleCrop().autoClone();
    }
    return GlideOptions.circleCropTransform3;
  }

  /**
   * @see RequestOptions#bitmapTransform(Transformation)
   */
  @CheckResult
  public static GlideOptions bitmapTransform(@NonNull Transformation<Bitmap> arg0) {
    return new GlideOptions().transform(arg0);
  }

  /**
   * @see RequestOptions#noTransformation()
   */
  @CheckResult
  public static GlideOptions noTransformation() {
    if (GlideOptions.noTransformation4 == null) {
      GlideOptions.noTransformation4 =
          new GlideOptions().dontTransform().autoClone();
    }
    return GlideOptions.noTransformation4;
  }

  /**
   * @see RequestOptions#option(Option, T)
   */
  @CheckResult
  public static <T> GlideOptions option(@NonNull Option<T> arg0, @NonNull T arg1) {
    return new GlideOptions().set(arg0, arg1);
  }

  /**
   * @see RequestOptions#decodeTypeOf(Class)
   */
  @CheckResult
  public static GlideOptions decodeTypeOf(@NonNull Class<?> arg0) {
    return new GlideOptions().decode(arg0);
  }

  /**
   * @see RequestOptions#formatOf(DecodeFormat)
   */
  @CheckResult
  public static GlideOptions formatOf(@NonNull DecodeFormat arg0) {
    return new GlideOptions().format(arg0);
  }

  /**
   * @see RequestOptions#frameOf(long)
   */
  @CheckResult
  public static GlideOptions frameOf(@IntRange(from = 0) long arg0) {
    return new GlideOptions().frame(arg0);
  }

  /**
   * @see RequestOptions#downsampleOf(DownsampleStrategy)
   */
  @CheckResult
  public static GlideOptions downsampleOf(@NonNull DownsampleStrategy arg0) {
    return new GlideOptions().downsample(arg0);
  }

  /**
   * @see RequestOptions#timeoutOf(int)
   */
  @CheckResult
  public static GlideOptions timeoutOf(@IntRange(from = 0) int arg0) {
    return new GlideOptions().timeout(arg0);
  }

  /**
   * @see RequestOptions#encodeQualityOf(int)
   */
  @CheckResult
  public static GlideOptions encodeQualityOf(@IntRange(from = 0, to = 100) int arg0) {
    return new GlideOptions().encodeQuality(arg0);
  }

  /**
   * @see RequestOptions#encodeFormatOf(CompressFormat)
   */
  @CheckResult
  public static GlideOptions encodeFormatOf(@NonNull Bitmap.CompressFormat arg0) {
    return new GlideOptions().encodeFormat(arg0);
  }

  /**
   * @see RequestOptions#noAnimation()
   */
  @CheckResult
  public static GlideOptions noAnimation() {
    if (GlideOptions.noAnimation5 == null) {
      GlideOptions.noAnimation5 =
          new GlideOptions().dontAnimate().autoClone();
    }
    return GlideOptions.noAnimation5;
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions sizeMultiplier(@FloatRange(from = 0.0, to = 1.0) float arg0) {
    return (GlideOptions) super.sizeMultiplier(arg0);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions useUnlimitedSourceGeneratorsPool(boolean flag) {
    return (GlideOptions) super.useUnlimitedSourceGeneratorsPool(flag);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions useAnimationPool(boolean flag) {
    return (GlideOptions) super.useAnimationPool(flag);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions onlyRetrieveFromCache(boolean flag) {
    return (GlideOptions) super.onlyRetrieveFromCache(flag);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions diskCacheStrategy(@NonNull DiskCacheStrategy arg0) {
    return (GlideOptions) super.diskCacheStrategy(arg0);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions priority(@NonNull Priority arg0) {
    return (GlideOptions) super.priority(arg0);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions placeholder(@Nullable Drawable arg0) {
    return (GlideOptions) super.placeholder(arg0);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions placeholder(@DrawableRes int arg0) {
    return (GlideOptions) super.placeholder(arg0);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions fallback(@Nullable Drawable arg0) {
    return (GlideOptions) super.fallback(arg0);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions fallback(@DrawableRes int arg0) {
    return (GlideOptions) super.fallback(arg0);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions error(@Nullable Drawable arg0) {
    return (GlideOptions) super.error(arg0);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions error(@DrawableRes int arg0) {
    return (GlideOptions) super.error(arg0);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions theme(@Nullable Resources.Theme arg0) {
    return (GlideOptions) super.theme(arg0);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions skipMemoryCache(boolean skip) {
    return (GlideOptions) super.skipMemoryCache(skip);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions override(int width, int height) {
    return (GlideOptions) super.override(width, height);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions override(int size) {
    return (GlideOptions) super.override(size);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions signature(@NonNull Key arg0) {
    return (GlideOptions) super.signature(arg0);
  }

  @Override
  @CheckResult
  public final GlideOptions clone() {
    return (GlideOptions) super.clone();
  }

  @Override
  @NonNull
  @CheckResult
  public final <T> GlideOptions set(@NonNull Option<T> arg0, @NonNull T arg1) {
    return (GlideOptions) super.set(arg0, arg1);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions decode(@NonNull Class<?> arg0) {
    return (GlideOptions) super.decode(arg0);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions encodeFormat(@NonNull Bitmap.CompressFormat arg0) {
    return (GlideOptions) super.encodeFormat(arg0);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions encodeQuality(@IntRange(from = 0, to = 100) int arg0) {
    return (GlideOptions) super.encodeQuality(arg0);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions frame(@IntRange(from = 0) long arg0) {
    return (GlideOptions) super.frame(arg0);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions format(@NonNull DecodeFormat arg0) {
    return (GlideOptions) super.format(arg0);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions disallowHardwareConfig() {
    return (GlideOptions) super.disallowHardwareConfig();
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions downsample(@NonNull DownsampleStrategy arg0) {
    return (GlideOptions) super.downsample(arg0);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions timeout(@IntRange(from = 0) int arg0) {
    return (GlideOptions) super.timeout(arg0);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions optionalCenterCrop() {
    return (GlideOptions) super.optionalCenterCrop();
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions centerCrop() {
    return (GlideOptions) super.centerCrop();
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions optionalFitCenter() {
    return (GlideOptions) super.optionalFitCenter();
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions fitCenter() {
    return (GlideOptions) super.fitCenter();
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions optionalCenterInside() {
    return (GlideOptions) super.optionalCenterInside();
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions centerInside() {
    return (GlideOptions) super.centerInside();
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions optionalCircleCrop() {
    return (GlideOptions) super.optionalCircleCrop();
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions circleCrop() {
    return (GlideOptions) super.circleCrop();
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions transform(@NonNull Transformation<Bitmap> arg0) {
    return (GlideOptions) super.transform(arg0);
  }

  @Override
  @SafeVarargs
  @SuppressWarnings("varargs")
  @NonNull
  @CheckResult
  public final GlideOptions transforms(@NonNull Transformation<Bitmap>... arg0) {
    return (GlideOptions) super.transforms(arg0);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions optionalTransform(@NonNull Transformation<Bitmap> arg0) {
    return (GlideOptions) super.optionalTransform(arg0);
  }

  @Override
  @NonNull
  @CheckResult
  public final <T> GlideOptions optionalTransform(@NonNull Class<T> arg0,
      @NonNull Transformation<T> arg1) {
    return (GlideOptions) super.optionalTransform(arg0, arg1);
  }

  @Override
  @NonNull
  @CheckResult
  public final <T> GlideOptions transform(@NonNull Class<T> arg0, @NonNull Transformation<T> arg1) {
    return (GlideOptions) super.transform(arg0, arg1);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions dontTransform() {
    return (GlideOptions) super.dontTransform();
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions dontAnimate() {
    return (GlideOptions) super.dontAnimate();
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideOptions apply(@NonNull RequestOptions arg0) {
    return (GlideOptions) super.apply(arg0);
  }

  @Override
  @NonNull
  public final GlideOptions lock() {
    return (GlideOptions) super.lock();
  }

  @Override
  @NonNull
  public final GlideOptions autoClone() {
    return (GlideOptions) super.autoClone();
  }

  /**
   * @see ExtensionWithOption#squareThumb(RequestOptions)
   */
  @CheckResult
  public GlideOptions squareThumb() {
    if (isAutoCloneEnabled()) {
      return clone().squareThumb();
    }
    ExtensionWithOption.squareThumb(this);
    return this;
  }

  /**
   * @see ExtensionWithOption#squareThumb(RequestOptions)
   */
  @CheckResult
  public static GlideOptions squareThumbOf() {
    return new GlideOptions().squareThumb();
  }
}
