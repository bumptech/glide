package com.bumptech.glide.test;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.manager.Lifecycle;
import com.bumptech.glide.manager.RequestManagerTreeNode;
import com.bumptech.glide.request.RequestOptions;
import java.io.File;
import java.lang.Class;
import java.lang.Object;
import java.lang.Override;
import java.lang.SuppressWarnings;

/**
 * Includes all additions from methods in {@link com.bumptech.glide.annotation.GlideExtension}s
 * annotated with {@link com.bumptech.glide.annotation.GlideType}
 *
 * <p>Generated code, do not modify
 */
@SuppressWarnings("deprecation")
public class GlideRequests extends RequestManager {
  public GlideRequests(Glide glide, Lifecycle lifecycle, RequestManagerTreeNode treeNode,
      Context context) {
    super(glide, lifecycle, treeNode, context);
  }

  @Override
  @CheckResult
  public <ResourceType> GlideRequest<ResourceType> as(Class<ResourceType> resourceClass) {
    return new GlideRequest<>(glide, this, resourceClass, context);
  }

  @Override
  public GlideRequests applyDefaultRequestOptions(RequestOptions requestOptions) {
    return (GlideRequests) super.applyDefaultRequestOptions(requestOptions);
  }

  @Override
  public GlideRequests setDefaultRequestOptions(RequestOptions requestOptions) {
    return (GlideRequests) super.setDefaultRequestOptions(requestOptions);
  }

  @Override
  @CheckResult
  public GlideRequest<Bitmap> asBitmap() {
    return (GlideRequest<Bitmap>) super.asBitmap();
  }

  @Override
  @CheckResult
  public GlideRequest<GifDrawable> asGif() {
    return (GlideRequest<GifDrawable>) super.asGif();
  }

  @Override
  @CheckResult
  public GlideRequest<Drawable> asDrawable() {
    return (GlideRequest<Drawable>) super.asDrawable();
  }

  @Override
  @CheckResult
  public GlideRequest<Drawable> load(@Nullable Object arg0) {
    return (GlideRequest<Drawable>) super.load(arg0);
  }

  @Override
  @CheckResult
  public GlideRequest<File> downloadOnly() {
    return (GlideRequest<File>) super.downloadOnly();
  }

  @Override
  @CheckResult
  public GlideRequest<File> download(@Nullable Object arg0) {
    return (GlideRequest<File>) super.download(arg0);
  }

  @Override
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
