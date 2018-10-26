package com.bumptech.glide.test;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.CheckResult;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.manager.Lifecycle;
import com.bumptech.glide.manager.RequestManagerTreeNode;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import java.io.File;
import java.lang.Class;
import java.lang.Deprecated;
import java.lang.Integer;
import java.lang.Number;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.net.URL;

/**
 * Includes all additions from methods in {@link com.bumptech.glide.annotation.GlideExtension}s
 * annotated with {@link com.bumptech.glide.annotation.GlideType}
 *
 * <p>Generated code, do not modify
 */
@SuppressWarnings("deprecation")
public class GlideRequests extends RequestManager {
  public GlideRequests(@NonNull Glide glide, @NonNull Lifecycle lifecycle,
      @NonNull RequestManagerTreeNode treeNode, @NonNull Context context) {
    super(glide, lifecycle, treeNode, context);
  }

  @Override
  @CheckResult
  @NonNull
  public <ResourceType> GlideRequest<ResourceType> as(@NonNull Class<ResourceType> resourceClass) {
    return new GlideRequest<>(glide, this, resourceClass, context);
  }

  /**
   * @see ExtensionWithType#asNumber(RequestBuilder)
   */
  @NonNull
  @CheckResult
  public GlideRequest<Number> asNumber() {
    return (GlideRequest<Number>) ExtensionWithType.asNumber(this.as(Number.class));
  }

  @Override
  @NonNull
  public synchronized GlideRequests applyDefaultRequestOptions(@NonNull RequestOptions options) {
    return (GlideRequests) super.applyDefaultRequestOptions(options);
  }

  @Override
  @NonNull
  public synchronized GlideRequests setDefaultRequestOptions(@NonNull RequestOptions options) {
    return (GlideRequests) super.setDefaultRequestOptions(options);
  }

  @Override
  @NonNull
  public synchronized GlideRequests addDefaultRequestListener(RequestListener<Object> listener) {
    return (GlideRequests) super.addDefaultRequestListener(listener);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<Bitmap> asBitmap() {
    return (GlideRequest<Bitmap>) super.asBitmap();
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<GifDrawable> asGif() {
    return (GlideRequest<GifDrawable>) super.asGif();
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<Drawable> asDrawable() {
    return (GlideRequest<Drawable>) super.asDrawable();
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<Drawable> load(@Nullable Bitmap bitmap) {
    return (GlideRequest<Drawable>) super.load(bitmap);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<Drawable> load(@Nullable Drawable drawable) {
    return (GlideRequest<Drawable>) super.load(drawable);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<Drawable> load(@Nullable String string) {
    return (GlideRequest<Drawable>) super.load(string);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<Drawable> load(@Nullable Uri uri) {
    return (GlideRequest<Drawable>) super.load(uri);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<Drawable> load(@Nullable File file) {
    return (GlideRequest<Drawable>) super.load(file);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<Drawable> load(@RawRes @DrawableRes @Nullable Integer id) {
    return (GlideRequest<Drawable>) super.load(id);
  }

  @Override
  @Deprecated
  @CheckResult
  public GlideRequest<Drawable> load(@Nullable URL url) {
    return (GlideRequest<Drawable>) super.load(url);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<Drawable> load(@Nullable byte[] bytes) {
    return (GlideRequest<Drawable>) super.load(bytes);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<Drawable> load(@Nullable Object o) {
    return (GlideRequest<Drawable>) super.load(o);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<File> downloadOnly() {
    return (GlideRequest<File>) super.downloadOnly();
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<File> download(@Nullable Object o) {
    return (GlideRequest<File>) super.download(o);
  }

  @Override
  @NonNull
  @CheckResult
  public GlideRequest<File> asFile() {
    return (GlideRequest<File>) super.asFile();
  }

  @Override
  protected void setRequestOptions(@NonNull RequestOptions toSet) {
    if (toSet instanceof com.bumptech.glide.test.GlideOptions) {
      super.setRequestOptions(toSet);
    } else {
      super.setRequestOptions(new com.bumptech.glide.test.GlideOptions().apply(toSet));
    }
  }
}
