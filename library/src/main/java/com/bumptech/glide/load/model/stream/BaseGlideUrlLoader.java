package com.bumptech.glide.load.model.stream;

import android.content.Context;
import android.text.TextUtils;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.Headers;
import com.bumptech.glide.load.model.ModelCache;
import com.bumptech.glide.load.model.ModelLoader;

import java.io.InputStream;

/**
 * A base class for loading images over http/https. Can be subclassed for use with any model that can be translated
 * in to {@link java.io.InputStream} data.
 *
 * @param <T> The type of the model.
 */
public abstract class BaseGlideUrlLoader<T> implements StreamModelLoader<T> {
    private final ModelLoader<GlideUrl, InputStream> concreteLoader;
    private final ModelCache<T, GlideUrl> modelCache;

    public BaseGlideUrlLoader(Context context) {
        this(context, null);
    }

    public BaseGlideUrlLoader(Context context, ModelCache<T, GlideUrl> modelCache) {
        this(Glide.buildModelLoader(GlideUrl.class, InputStream.class, context), modelCache);
    }

    public BaseGlideUrlLoader(ModelLoader<GlideUrl, InputStream> concreteLoader) {
        this(concreteLoader, null);
    }

    public BaseGlideUrlLoader(ModelLoader<GlideUrl, InputStream> concreteLoader, ModelCache<T, GlideUrl> modelCache) {
        this.concreteLoader = concreteLoader;
        this.modelCache = modelCache;
    }

    @Override
    public DataFetcher<InputStream> getResourceFetcher(T model, int width, int height) {
        GlideUrl result = null;
        if (modelCache != null) {
            result = modelCache.get(model, width, height);
        }

        if (result == null) {
            String stringURL = getUrl(model, width, height);
            if (TextUtils.isEmpty(stringURL)) {
               return null;
            }

            result = new GlideUrl(stringURL, getHeaders(model, width, height));

            if (modelCache != null) {
                modelCache.put(model, width, height, result);
            }
        }

        return concreteLoader.getResourceFetcher(result, width, height);
    }

    /**
     * Get a valid url http:// or https:// for the given model and dimensions as a string.
     *
     * @param model The model.
     * @param width The width in pixels of the view/target the image will be loaded into.
     * @param height The height in pixels of the view/target the image will be loaded into.
     * @return The String url.
     */
    protected abstract String getUrl(T model, int width, int height);

    /**
     * Get the headers for the given model and dimensions as a map of strings to sets of strings.
     *
     * @param model The model.
     * @param width The width in pixels of the view/target the image will be loaded into.
     * @param height The height in pixels of the view/target the image will be loaded into.
     * @return The Headers object containing the headers, or null if no headers should be added.
     */
    protected Headers getHeaders(T model, int width, int height) {
        return Headers.DEFAULT;
    }
}
