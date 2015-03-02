package com.bumptech.glide.load.model.stream;

import android.text.TextUtils;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelCache;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.signature.ObjectKey;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A base class for loading data over http/https. Can be subclassed for use with any model that can
 * be translated in to {@link java.io.InputStream} data.
 *
 * @param <Model> The type of the model.
 */
public abstract class BaseGlideUrlLoader<Model> implements ModelLoader<Model, InputStream> {
  private final ModelLoader<GlideUrl, InputStream> concreteLoader;
  private final ModelCache<Model, GlideUrl> modelCache;

  protected BaseGlideUrlLoader(ModelLoader<GlideUrl, InputStream> concreteLoader) {
    this(concreteLoader, null);
  }

  protected BaseGlideUrlLoader(ModelLoader<GlideUrl, InputStream> concreteLoader,
      ModelCache<Model, GlideUrl> modelCache) {
    this.concreteLoader = concreteLoader;
    this.modelCache = modelCache;
  }

  @Override
  public LoadData<InputStream> buildLoadData(Model model, int width, int height) {
    GlideUrl result = null;
    if (modelCache != null) {
      result = modelCache.get(model, width, height);
    }

    if (result == null) {
      String stringURL = getUrl(model, width, height);
      if (TextUtils.isEmpty(stringURL)) {
        return null;
      }

      result = new GlideUrl(stringURL);

      if (modelCache != null) {
        modelCache.put(model, width, height, result);
      }
    }

    List<String> alternateUrls = getAlternateUrls(model, width, height);
    LoadData<InputStream> concreteLoaderData = concreteLoader.buildLoadData(result, width, height);
    if (alternateUrls.isEmpty()) {
      return concreteLoaderData;
    } else {
      return new LoadData<>(concreteLoaderData.sourceKey, getAlternateKeys(alternateUrls),
          concreteLoaderData.fetcher);
    }
  }

  private static List<Key> getAlternateKeys(List<String> alternateUrls) {
    List<Key> result = new ArrayList<>(alternateUrls.size());
    for (String alternate : alternateUrls) {
      result.add(new ObjectKey(alternate));
    }
    return result;
  }

  /**
   * Get a valid url http:// or https:// for the given model and dimensions as a string.
   *
   * @param model  The model.
   * @param width  The width in pixels of the view/target the image will be loaded into.
   * @param height The height in pixels of the view/target the image will be loaded into.
   * @return The String url.
   */
  protected abstract String getUrl(Model model, int width, int height);

  protected List<String> getAlternateUrls(Model model, int width, int height) {
    return Collections.emptyList();
  }
}
