package com.bumptech.glide.loader.bitmap.model.stream;

import android.content.Context;
import com.bumptech.glide.Glide;
import com.bumptech.glide.loader.bitmap.model.Cache;
import com.bumptech.glide.loader.bitmap.model.ModelLoader;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * A base class for loading images over http/https. Can be subclassed for use with any model that can be translated
 * in to an image.
 *
 * @param <T> The type of the model
 */
public abstract class BaseUrlLoader<T> implements StreamModelLoader<T> {
    private final ModelLoader<URL, InputStream> concreteLoader;
    private final Cache<URL> modelCache;

    public BaseUrlLoader(Context context) {
        this(context, null);
    }

    public BaseUrlLoader(Context context, Cache<URL> modelCache) {
        this(Glide.buildModelLoader(URL.class, InputStream.class, context), modelCache);
    }

    @SuppressWarnings("unused")
    public BaseUrlLoader(ModelLoader<URL, InputStream> concreteLoader) {
        this(concreteLoader, null);
    }

    public BaseUrlLoader(ModelLoader<URL, InputStream> concreteLoader, Cache<URL> modelCache) {
        this.concreteLoader = concreteLoader;
        this.modelCache = modelCache;
    }

    @Override
    public ResourceFetcher<InputStream> getResourceFetcher(T model, int width, int height) {
        final String id = getId(model);
        URL result = null;
        if (modelCache != null) {
            result = modelCache.get(id, width, height);
        }

        if (result == null) {
            String stringURL = getUrl(model, width, height);
            try {
                result = new URL(stringURL);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            if (result == null) {
                throw new IllegalArgumentException("Invalid URL for model=" + model + " url=" + stringURL);
            }

            if (modelCache != null) {
                modelCache.put(id, width, height, result);
            }
        }

        return concreteLoader.getResourceFetcher(result, width, height);
    }

    /**
     * Get a valid url http:// or https:// for the given model and dimensions as a string
     *
     * @param model The model
     * @param width The width of the view/target the image will be loaded into
     * @param height The height of the view/target the image will be loaded into
     * @return The String url
     */
    protected abstract String getUrl(T model, int width, int height);
}
