package com.bumptech.glide.test;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.CheckResult;
import android.support.annotation.DrawableRes;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.TransitionOptions;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy;
import com.bumptech.glide.request.BaseRequestOptions;
import com.bumptech.glide.request.RequestListener;
import java.io.File;
import java.lang.Class;
import java.lang.Cloneable;
import java.lang.Deprecated;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.SafeVarargs;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.net.URL;

/**
 * Contains all public methods from {@link RequestBuilder<TranscodeType>}, all options from
 * {@link com.bumptech.glide.request.RequestOptions} and all generated options from
 * {@link com.bumptech.glide.annotation.GlideOption} in annotated methods in
 * {@link com.bumptech.glide.annotation.GlideExtension} annotated classes.
 *
 * <p>Generated code, do not modify.
 *
 * @see RequestBuilder<TranscodeType>
 * @see com.bumptech.glide.request.RequestOptions
 */
@SuppressWarnings({
    "unused",
    "deprecation"
})
public class GlideRequest<TranscodeType> extends RequestBuilder<TranscodeType> implements Cloneable {
  GlideRequest(@NonNull Class<TranscodeType> transcodeClass, @NonNull RequestBuilder<?> other) {
    super(transcodeClass, other);
  }

  GlideRequest(@NonNull Glide glide, @NonNull RequestManager requestManager,
      @NonNull Class<TranscodeType> transcodeClass, @NonNull Context context) {
    super(glide, requestManager ,transcodeClass, context);
  }

  @Override
  @CheckResult
  @NonNull
  protected GlideRequest<File> getDownloadOnlyRequest() {
    return new GlideRequest<>(File.class, this).apply(DOWNLOAD_ONLY_OPTIONS);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> sizeMultiplier(@FloatRange(from = 0.0, to = 1.0) float value) {
    return (GlideRequest<TranscodeType>) super.sizeMultiplier(value);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> useUnlimitedSourceGeneratorsPool(boolean flag) {
    return (GlideRequest<TranscodeType>) super.useUnlimitedSourceGeneratorsPool(flag);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> useAnimationPool(boolean flag) {
    return (GlideRequest<TranscodeType>) super.useAnimationPool(flag);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> onlyRetrieveFromCache(boolean flag) {
    return (GlideRequest<TranscodeType>) super.onlyRetrieveFromCache(flag);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> diskCacheStrategy(@NonNull DiskCacheStrategy strategy) {
    return (GlideRequest<TranscodeType>) super.diskCacheStrategy(strategy);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> priority(@NonNull Priority priority) {
    return (GlideRequest<TranscodeType>) super.priority(priority);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> placeholder(@Nullable Drawable drawable) {
    return (GlideRequest<TranscodeType>) super.placeholder(drawable);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> placeholder(@DrawableRes int id) {
    return (GlideRequest<TranscodeType>) super.placeholder(id);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> fallback(@Nullable Drawable drawable) {
    return (GlideRequest<TranscodeType>) super.fallback(drawable);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> fallback(@DrawableRes int id) {
    return (GlideRequest<TranscodeType>) super.fallback(id);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> error(@Nullable Drawable drawable) {
    return (GlideRequest<TranscodeType>) super.error(drawable);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> error(@DrawableRes int id) {
    return (GlideRequest<TranscodeType>) super.error(id);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> theme(@Nullable Resources.Theme theme) {
    return (GlideRequest<TranscodeType>) super.theme(theme);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> skipMemoryCache(boolean skip) {
    return (GlideRequest<TranscodeType>) super.skipMemoryCache(skip);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> override(int width, int height) {
    return (GlideRequest<TranscodeType>) super.override(width, height);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> override(int size) {
    return (GlideRequest<TranscodeType>) super.override(size);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> signature(@NonNull Key key) {
    return (GlideRequest<TranscodeType>) super.signature(key);
  }

  @Override
  @NonNull
  @CheckResult
  public final <Y> GlideRequest<TranscodeType> set(@NonNull Option<Y> option, @NonNull Y y) {
    return (GlideRequest<TranscodeType>) super.set(option, y);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> decode(@NonNull Class<?> clazz) {
    return (GlideRequest<TranscodeType>) super.decode(clazz);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> encodeFormat(@NonNull Bitmap.CompressFormat format) {
    return (GlideRequest<TranscodeType>) super.encodeFormat(format);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> encodeQuality(@IntRange(from = 0, to = 100) int value) {
    return (GlideRequest<TranscodeType>) super.encodeQuality(value);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> frame(@IntRange(from = 0) long value) {
    return (GlideRequest<TranscodeType>) super.frame(value);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> format(@NonNull DecodeFormat format) {
    return (GlideRequest<TranscodeType>) super.format(format);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> disallowHardwareConfig() {
    return (GlideRequest<TranscodeType>) super.disallowHardwareConfig();
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> downsample(@NonNull DownsampleStrategy strategy) {
    return (GlideRequest<TranscodeType>) super.downsample(strategy);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> timeout(@IntRange(from = 0) int value) {
    return (GlideRequest<TranscodeType>) super.timeout(value);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> optionalCenterCrop() {
    return (GlideRequest<TranscodeType>) super.optionalCenterCrop();
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> centerCrop() {
    return (GlideRequest<TranscodeType>) super.centerCrop();
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> optionalFitCenter() {
    return (GlideRequest<TranscodeType>) super.optionalFitCenter();
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> fitCenter() {
    return (GlideRequest<TranscodeType>) super.fitCenter();
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> optionalCenterInside() {
    return (GlideRequest<TranscodeType>) super.optionalCenterInside();
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> centerInside() {
    return (GlideRequest<TranscodeType>) super.centerInside();
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> optionalCircleCrop() {
    return (GlideRequest<TranscodeType>) super.optionalCircleCrop();
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> circleCrop() {
    return (GlideRequest<TranscodeType>) super.circleCrop();
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> transform(@NonNull Transformation<Bitmap> transformation) {
    return (GlideRequest<TranscodeType>) super.transform(transformation);
  }

  @Override
  @SafeVarargs
  @SuppressWarnings("varargs")
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> transforms(@NonNull Transformation<Bitmap>... transformations) {
    return (GlideRequest<TranscodeType>) super.transforms(transformations);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> optionalTransform(@NonNull Transformation<Bitmap> transformation) {
    return (GlideRequest<TranscodeType>) super.optionalTransform(transformation);
  }

  @Override
  @NonNull
  @CheckResult
  public final <Y> GlideRequest<TranscodeType> optionalTransform(@NonNull Class<Y> clazz,
      @NonNull Transformation<Y> transformation) {
    return (GlideRequest<TranscodeType>) super.optionalTransform(clazz, transformation);
  }

  @Override
  @NonNull
  @CheckResult
  public final <Y> GlideRequest<TranscodeType> transform(@NonNull Class<Y> clazz,
      @NonNull Transformation<Y> transformation) {
    return (GlideRequest<TranscodeType>) super.transform(clazz, transformation);
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> dontTransform() {
    return (GlideRequest<TranscodeType>) super.dontTransform();
  }

  @Override
  @NonNull
  @CheckResult
  public final GlideRequest<TranscodeType> dontAnimate() {
    return (GlideRequest<TranscodeType>) super.dontAnimate();
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> apply(@NonNull BaseRequestOptions<?> options) {
    return (GlideRequest<TranscodeType>) super.apply(options);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> transition(@NonNull TransitionOptions<?, ? super TranscodeType> options) {
    return (GlideRequest<TranscodeType>) super.transition(options);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> listener(@Nullable RequestListener<TranscodeType> listener) {
    return (GlideRequest<TranscodeType>) super.listener(listener);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> addListener(@Nullable RequestListener<TranscodeType> listener) {
    return (GlideRequest<TranscodeType>) super.addListener(listener);
  }

  @Override
  @NonNull
  public GlideRequest<TranscodeType> error(@Nullable RequestBuilder<TranscodeType> builder) {
    return (GlideRequest<TranscodeType>) super.error(builder);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> thumbnail(@Nullable RequestBuilder<TranscodeType> builder) {
    return (GlideRequest<TranscodeType>) super.thumbnail(builder);
  }

  @Override
  @NonNull
  @CheckResult
  @SafeVarargs
  @SuppressWarnings("varargs")
  public final GlideRequest<TranscodeType> thumbnail(@Nullable RequestBuilder<TranscodeType>... builders) {
    return (GlideRequest<TranscodeType>) super.thumbnail(builders);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> thumbnail(float sizeMultiplier) {
    return (GlideRequest<TranscodeType>) super.thumbnail(sizeMultiplier);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> load(@Nullable Object o) {
    return (GlideRequest<TranscodeType>) super.load(o);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> load(@Nullable Bitmap bitmap) {
    return (GlideRequest<TranscodeType>) super.load(bitmap);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> load(@Nullable Drawable drawable) {
    return (GlideRequest<TranscodeType>) super.load(drawable);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> load(@Nullable String string) {
    return (GlideRequest<TranscodeType>) super.load(string);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> load(@Nullable Uri uri) {
    return (GlideRequest<TranscodeType>) super.load(uri);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> load(@Nullable File file) {
    return (GlideRequest<TranscodeType>) super.load(file);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> load(@RawRes @DrawableRes @Nullable Integer id) {
    return (GlideRequest<TranscodeType>) super.load(id);
  }

  @Override
  @Deprecated
  @CheckResult
  public GlideRequest<TranscodeType> load(@Nullable URL url) {
    return (GlideRequest<TranscodeType>) super.load(url);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> load(@Nullable byte[] bytes) {
    return (GlideRequest<TranscodeType>) super.load(bytes);
  }

  @Override
  @CheckResult
  public GlideRequest<TranscodeType> clone() {
    return (GlideRequest<TranscodeType>) super.clone();
  }
}
