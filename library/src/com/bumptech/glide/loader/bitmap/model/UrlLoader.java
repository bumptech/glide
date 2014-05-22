package com.bumptech.glide.loader.bitmap.model;

import com.bumptech.glide.loader.GlideUrl;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;

import java.net.URL;

public class UrlLoader<T> implements ModelLoader<URL, T> {
    private final ModelLoader<GlideUrl, T> glideUrlLoader;

    public UrlLoader(ModelLoader<GlideUrl, T> glideUrlLoader) {
        this.glideUrlLoader = glideUrlLoader;
    }

    @Override
    public ResourceFetcher<T> getResourceFetcher(URL model, int width, int height) {
        return glideUrlLoader.getResourceFetcher(new GlideUrl(model), width, height);
    }

    @Override
    public String getId(URL model) {
        return model.toString();
    }
}
