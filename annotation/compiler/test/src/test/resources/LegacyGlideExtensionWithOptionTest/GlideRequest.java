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
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
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
 * {@link RequestOptions} and all generated options from
 * {@link com.bumptech.glide.annotation.GlideOption} in annotated methods in
 * {@link com.bumptech.glide.annotation.GlideExtension} annotated classes.
 *
 * <p>Generated code, do not modify.
 *
 * @see RequestBuilder<TranscodeType>
 * @see RequestOptions
 */
@SuppressWarnings({
    "unused",
    "deprecation"
})
public class GlideRequest<TranscodeType> extends RequestBuilder<TranscodeType> implements Cloneable {
  GlideRequest(Class<TranscodeType> transcodeClass, RequestBuilder<?> other) {
    super(transcodeClass, other);
  }

  GlideRequest(Glide glide, RequestManager requestManager, Class<TranscodeType> transcodeClass,
      Context context) {
    super(glide, requestManager ,transcodeClass, context);
  }

  @Override
  @CheckResult
  protected GlideRequest<File> getDownloadOnlyRequest() {
    return new GlideRequest<>(File.class, this).apply(DOWNLOAD_ONLY_OPTIONS);
  }

  /**
   * @see GlideOptions#sizeMultiplier(float)
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> sizeMultiplier(@FloatRange(from = 0.0, to = 1.0) float arg0) {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).sizeMultiplier(arg0);
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).sizeMultiplier(arg0);
    }
    return this;
  }

  /**
   * @see GlideOptions#useUnlimitedSourceGeneratorsPool(boolean)
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> useUnlimitedSourceGeneratorsPool(boolean flag) {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).useUnlimitedSourceGeneratorsPool(flag);
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).useUnlimitedSourceGeneratorsPool(flag);
    }
    return this;
  }

  /**
   * @see GlideOptions#useAnimationPool(boolean)
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> useAnimationPool(boolean flag) {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).useAnimationPool(flag);
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).useAnimationPool(flag);
    }
    return this;
  }

  /**
   * @see GlideOptions#onlyRetrieveFromCache(boolean)
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> onlyRetrieveFromCache(boolean flag) {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).onlyRetrieveFromCache(flag);
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).onlyRetrieveFromCache(flag);
    }
    return this;
  }

  /**
   * @see GlideOptions#diskCacheStrategy(DiskCacheStrategy)
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> diskCacheStrategy(@NonNull DiskCacheStrategy arg0) {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).diskCacheStrategy(arg0);
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).diskCacheStrategy(arg0);
    }
    return this;
  }

  /**
   * @see GlideOptions#priority(Priority)
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> priority(@NonNull Priority arg0) {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).priority(arg0);
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).priority(arg0);
    }
    return this;
  }

  /**
   * @see GlideOptions#placeholder(Drawable)
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> placeholder(@Nullable Drawable arg0) {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).placeholder(arg0);
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).placeholder(arg0);
    }
    return this;
  }

  /**
   * @see GlideOptions#placeholder(int)
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> placeholder(@DrawableRes int arg0) {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).placeholder(arg0);
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).placeholder(arg0);
    }
    return this;
  }

  /**
   * @see GlideOptions#fallback(Drawable)
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> fallback(@Nullable Drawable arg0) {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).fallback(arg0);
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).fallback(arg0);
    }
    return this;
  }

  /**
   * @see GlideOptions#fallback(int)
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> fallback(@DrawableRes int arg0) {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).fallback(arg0);
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).fallback(arg0);
    }
    return this;
  }

  /**
   * @see GlideOptions#error(Drawable)
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> error(@Nullable Drawable arg0) {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).error(arg0);
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).error(arg0);
    }
    return this;
  }

  /**
   * @see GlideOptions#error(int)
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> error(@DrawableRes int arg0) {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).error(arg0);
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).error(arg0);
    }
    return this;
  }

  /**
   * @see GlideOptions#theme(Resources.Theme)
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> theme(@Nullable Resources.Theme arg0) {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).theme(arg0);
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).theme(arg0);
    }
    return this;
  }

  /**
   * @see GlideOptions#skipMemoryCache(boolean)
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> skipMemoryCache(boolean skip) {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).skipMemoryCache(skip);
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).skipMemoryCache(skip);
    }
    return this;
  }

  /**
   * @see GlideOptions#override(int, int)
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> override(int width, int height) {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).override(width, height);
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).override(width, height);
    }
    return this;
  }

  /**
   * @see GlideOptions#override(int)
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> override(int size) {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).override(size);
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).override(size);
    }
    return this;
  }

  /**
   * @see GlideOptions#signature(Key)
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> signature(@NonNull Key arg0) {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).signature(arg0);
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).signature(arg0);
    }
    return this;
  }

  /**
   * @see GlideOptions#set(Option<T>, T)
   */
  @NonNull
  @CheckResult
  public <T> GlideRequest<TranscodeType> set(@NonNull Option<T> arg0, @NonNull T arg1) {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).set(arg0, arg1);
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).set(arg0, arg1);
    }
    return this;
  }

  /**
   * @see GlideOptions#decode(Class<?>)
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> decode(@NonNull Class<?> arg0) {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).decode(arg0);
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).decode(arg0);
    }
    return this;
  }

  /**
   * @see GlideOptions#encodeFormat(Bitmap.CompressFormat)
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> encodeFormat(@NonNull Bitmap.CompressFormat arg0) {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).encodeFormat(arg0);
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).encodeFormat(arg0);
    }
    return this;
  }

  /**
   * @see GlideOptions#encodeQuality(int)
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> encodeQuality(@IntRange(from = 0, to = 100) int arg0) {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).encodeQuality(arg0);
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).encodeQuality(arg0);
    }
    return this;
  }

  /**
   * @see GlideOptions#frame(long)
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> frame(@IntRange(from = 0) long arg0) {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).frame(arg0);
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).frame(arg0);
    }
    return this;
  }

  /**
   * @see GlideOptions#format(DecodeFormat)
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> format(@NonNull DecodeFormat arg0) {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).format(arg0);
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).format(arg0);
    }
    return this;
  }

  /**
   * @see GlideOptions#disallowHardwareConfig()
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> disallowHardwareConfig() {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).disallowHardwareConfig();
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).disallowHardwareConfig();
    }
    return this;
  }

  /**
   * @see GlideOptions#downsample(DownsampleStrategy)
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> downsample(@NonNull DownsampleStrategy arg0) {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).downsample(arg0);
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).downsample(arg0);
    }
    return this;
  }

  /**
   * @see GlideOptions#timeout(int)
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> timeout(@IntRange(from = 0) int arg0) {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).timeout(arg0);
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).timeout(arg0);
    }
    return this;
  }

  /**
   * @see GlideOptions#optionalCenterCrop()
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> optionalCenterCrop() {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).optionalCenterCrop();
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).optionalCenterCrop();
    }
    return this;
  }

  /**
   * @see GlideOptions#centerCrop()
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> centerCrop() {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).centerCrop();
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).centerCrop();
    }
    return this;
  }

  /**
   * @see GlideOptions#optionalFitCenter()
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> optionalFitCenter() {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).optionalFitCenter();
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).optionalFitCenter();
    }
    return this;
  }

  /**
   * @see GlideOptions#fitCenter()
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> fitCenter() {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).fitCenter();
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).fitCenter();
    }
    return this;
  }

  /**
   * @see GlideOptions#optionalCenterInside()
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> optionalCenterInside() {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).optionalCenterInside();
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).optionalCenterInside();
    }
    return this;
  }

  /**
   * @see GlideOptions#centerInside()
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> centerInside() {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).centerInside();
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).centerInside();
    }
    return this;
  }

  /**
   * @see GlideOptions#optionalCircleCrop()
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> optionalCircleCrop() {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).optionalCircleCrop();
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).optionalCircleCrop();
    }
    return this;
  }

  /**
   * @see GlideOptions#circleCrop()
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> circleCrop() {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).circleCrop();
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).circleCrop();
    }
    return this;
  }

  /**
   * @see GlideOptions#transform(Transformation<Bitmap>)
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> transform(@NonNull Transformation<Bitmap> arg0) {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).transform(arg0);
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).transform(arg0);
    }
    return this;
  }

  /**
   * @see GlideOptions#transforms(Transformation<Bitmap>[])
   */
  @NonNull
  @CheckResult
  @SuppressWarnings({
      "unchecked",
      "varargs"
  })
  public GlideRequest<TranscodeType> transforms(@NonNull Transformation<Bitmap>... arg0) {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).transforms(arg0);
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).transforms(arg0);
    }
    return this;
  }

  /**
   * @see GlideOptions#optionalTransform(Transformation<Bitmap>)
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> optionalTransform(@NonNull Transformation<Bitmap> arg0) {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).optionalTransform(arg0);
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).optionalTransform(arg0);
    }
    return this;
  }

  /**
   * @see GlideOptions#optionalTransform(Class<T>, Transformation<T>)
   */
  @NonNull
  @CheckResult
  public <T> GlideRequest<TranscodeType> optionalTransform(@NonNull Class<T> arg0,
      @NonNull Transformation<T> arg1) {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).optionalTransform(arg0, arg1);
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).optionalTransform(arg0, arg1);
    }
    return this;
  }

  /**
   * @see GlideOptions#transform(Class<T>, Transformation<T>)
   */
  @NonNull
  @CheckResult
  public <T> GlideRequest<TranscodeType> transform(@NonNull Class<T> arg0,
      @NonNull Transformation<T> arg1) {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).transform(arg0, arg1);
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).transform(arg0, arg1);
    }
    return this;
  }

  /**
   * @see GlideOptions#dontTransform()
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> dontTransform() {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).dontTransform();
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).dontTransform();
    }
    return this;
  }

  /**
   * @see GlideOptions#dontAnimate()
   */
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> dontAnimate() {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).dontAnimate();
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).dontAnimate();
    }
    return this;
  }

  /**
   * @see GlideOptions#squareThumb()
   */
  @CheckResult
  public GlideRequest<TranscodeType> squareThumb() {
    if (getMutableOptions() instanceof GlideOptions) {
      this.requestOptions = ((GlideOptions) getMutableOptions()).squareThumb();
    } else {
      this.requestOptions = new GlideOptions().apply(this.requestOptions).squareThumb();
    }
    return this;
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> apply(@NonNull RequestOptions arg0) {
    return (GlideRequest<TranscodeType>) super.apply(arg0);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> transition(@NonNull TransitionOptions<?, ? super TranscodeType> arg0) {
    return (GlideRequest<TranscodeType>) super.transition(arg0);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> listener(@Nullable RequestListener<TranscodeType> arg0) {
    return (GlideRequest<TranscodeType>) super.listener(arg0);
  }

  @Override
  @NonNull
  public GlideRequest<TranscodeType> error(@Nullable RequestBuilder<TranscodeType> arg0) {
    return (GlideRequest<TranscodeType>) super.error(arg0);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> thumbnail(@Nullable RequestBuilder<TranscodeType> arg0) {
    return (GlideRequest<TranscodeType>) super.thumbnail(arg0);
  }

  @Override
  @NonNull
  @CheckResult
  @SafeVarargs
  @SuppressWarnings("varargs")
  public final GlideRequest<TranscodeType> thumbnail(@Nullable RequestBuilder<TranscodeType>... arg0) {
    return (GlideRequest<TranscodeType>) super.thumbnail(arg0);
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
  public GlideRequest<TranscodeType> load(@Nullable Object arg0) {
    return (GlideRequest<TranscodeType>) super.load(arg0);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> load(@Nullable Bitmap arg0) {
    return (GlideRequest<TranscodeType>) super.load(arg0);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> load(@Nullable Drawable arg0) {
    return (GlideRequest<TranscodeType>) super.load(arg0);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> load(@Nullable String arg0) {
    return (GlideRequest<TranscodeType>) super.load(arg0);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> load(@Nullable Uri arg0) {
    return (GlideRequest<TranscodeType>) super.load(arg0);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> load(@Nullable File arg0) {
    return (GlideRequest<TranscodeType>) super.load(arg0);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> load(@RawRes @DrawableRes @Nullable Integer arg0) {
    return (GlideRequest<TranscodeType>) super.load(arg0);
  }

  @Override
  @Deprecated
  @CheckResult
  public GlideRequest<TranscodeType> load(@Nullable URL arg0) {
    return (GlideRequest<TranscodeType>) super.load(arg0);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<TranscodeType> load(@Nullable byte[] arg0) {
    return (GlideRequest<TranscodeType>) super.load(arg0);
  }

  @Override
  @CheckResult
  public GlideRequest<TranscodeType> clone() {
    return (GlideRequest<TranscodeType>) super.clone();
  }
}
