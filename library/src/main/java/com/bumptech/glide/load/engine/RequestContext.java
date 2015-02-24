package com.bumptech.glide.load.engine;

import android.content.ContextWrapper;
import android.graphics.drawable.Drawable;

import com.bumptech.glide.GlideContext;
import com.bumptech.glide.Priority;
import com.bumptech.glide.Registry;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.data.DataFetcherSet;
import com.bumptech.glide.load.data.DataRewinder;
import com.bumptech.glide.request.BaseRequestOptions;
import com.bumptech.glide.util.Util;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Context for individual requests and decodes that contains and exposes classes necessary to
 * obtain, decode, and encode resources.
 *
 * @param <TranscodeClass> The type of resources returned using classes from this object.
 */
public class RequestContext<TranscodeClass> extends ContextWrapper {
  private final GlideContext glideContext;
  private final Object model;
  private final Class<TranscodeClass> transcodeClass;
  private final BaseRequestOptions<?> requestOptions;
  private final Priority priority;
  private final int overrideWidth;
  private final int overrideHeight;
  private DataFetcherSet<?> fetchers;
  private Drawable errorDrawable;
  private Drawable placeholderDrawable;

  public RequestContext(GlideContext glideContext, Object model,
      Class<TranscodeClass> transcodeClass, BaseRequestOptions<?> requestOptions, Priority priority,
      int overrideWidth, int overrideHeight) {
    super(glideContext);
    this.glideContext = glideContext;
    this.model = model;
    this.transcodeClass = transcodeClass;
    this.requestOptions = requestOptions;
    this.priority = priority;
    this.overrideWidth = overrideWidth;
    this.overrideHeight = overrideHeight;
  }

  <Data> LoadPath<Data, ?, TranscodeClass> getLoadPath(Class<Data> dataClass) {
    return glideContext.getRegistry().getLoadPath(dataClass, getResourceClass(), transcodeClass);
  }

  void buildDataFetchers(int width, int height) {
    Util.assertMainThread();
    if (fetchers == null) {
      fetchers = glideContext.getRegistry().getDataFetchers(model, width, height);
    }
  }

  DataFetcherSet<?> getDataFetchers() {
    if (fetchers == null) {
      throw new IllegalStateException("Must call buildDataFetchers first");
    }
    return fetchers;
  }

  String getId() {
    if (fetchers == null) {
      throw new IllegalStateException("Must call buildDataFetchers first");
    }
    return fetchers.getId();
  }

  Key getSignature() {
    return requestOptions.getSignature();
  }

  List<Class<?>> getRegisteredResourceClasses() {
    return glideContext.getRegistry()
        .getRegisteredResourceClasses(model.getClass(), requestOptions.getResourceClass());
  }

  Class<?> getResourceClass() {
    return requestOptions.getResourceClass();
  }

  <DecodedResource> Transformation<DecodedResource> getTransformation(
      Class<DecodedResource> decodedResourceClass) {
    return requestOptions.getTransformation(decodedResourceClass);
  }

  Map<String, Object> getOptions() {
    return requestOptions.getOptions();
  }

  boolean isMemoryCacheable() {
    return requestOptions.isMemoryCacheable();
  }

  DiskCacheStrategy getDiskCacheStrategy() {
    return requestOptions.getDiskCacheStrategy();
  }

  Priority getPriority() {
    return priority;
  }

  <X> DataRewinder<X> getRewinder(X data) {
    return glideContext.getRegistry().getRewinder(data);
  }

  boolean isResourceEncoderAvailable(Resource<?> resource) {
    return glideContext.getRegistry().isResourceEncoderAvailable(resource);
  }

  <ResourceClass> ResourceEncoder<ResourceClass> getResultEncoder(Resource<ResourceClass> resource)
      throws Registry.NoResultEncoderAvailableException {
    return glideContext.getRegistry().getResultEncoder(resource);
  }

  <X> Encoder<X> getSourceEncoder(X data) throws Registry.NoSourceEncoderAvailableException {
    return glideContext.getRegistry().getSourceEncoder(data);
  }

  DataFetcherSet<?> getDataFetchers(File file, int width, int height)
      throws Registry.NoModelLoaderAvailableException {
    return glideContext.getRegistry().getDataFetchers(file, width, height);
  }

  public int getOverrideWidth() {
    return overrideWidth;
  }

  public int getOverrideHeight() {
    return overrideHeight;
  }

  public Object getModel() {
    return model;
  }

  public Class<TranscodeClass> getTranscodeClass() {
    return transcodeClass;
  }

  public float getSizeMultiplier() {
    return requestOptions.getSizeMultiplier();
  }

  public Drawable getErrorDrawable() {
    if (errorDrawable == null) {
      errorDrawable = requestOptions.getErrorPlaceholder();
      if (errorDrawable == null && requestOptions.getErrorId() > 0) {
        errorDrawable = getResources().getDrawable(requestOptions.getErrorId());
      }
    }
    return errorDrawable;
  }

  public Drawable getPlaceholderDrawable() {
     if (placeholderDrawable == null) {
      placeholderDrawable = requestOptions.getPlaceholderDrawable();
      if (placeholderDrawable == null && requestOptions.getPlaceholderId() > 0) {
        placeholderDrawable = getResources().getDrawable(requestOptions.getPlaceholderId());
      }
    }
    return placeholderDrawable;
  }
}
