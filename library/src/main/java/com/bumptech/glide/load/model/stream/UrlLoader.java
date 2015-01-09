package com.bumptech.glide.load.model.stream;

import android.content.Context;

import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

import java.io.InputStream;
import java.net.URL;

/**
 * A wrapper class that translates {@link java.net.URL} objects into {@link com.bumptech.glide.load.model.GlideUrl}
 * objects and then uses the wrapped {@link com.bumptech.glide.load.model.ModelLoader} for
 * {@link com.bumptech.glide.load.model.GlideUrl}s to load the data.
 */
public class UrlLoader implements ModelLoader<URL, InputStream> {
    private final ModelLoader<GlideUrl, InputStream> glideUrlLoader;

    public UrlLoader(ModelLoader<GlideUrl, InputStream> glideUrlLoader) {
        this.glideUrlLoader = glideUrlLoader;
    }

    @Override
    public DataFetcher<InputStream> getDataFetcher(URL model, int width, int height) {
        return glideUrlLoader.getDataFetcher(new GlideUrl(model), width, height);
    }

    @Override
    public boolean handles(URL model) {
        return true;
    }

    /**
     * Factory for loading {@link InputStream}s from {@link URL}s.
     */
    public static class StreamFactory implements ModelLoaderFactory<URL, InputStream> {

        @Override
        public ModelLoader<URL, InputStream> build(Context context, MultiModelLoaderFactory multiFactory) {
            return new UrlLoader(multiFactory.build(GlideUrl.class, InputStream.class));
        }

        @Override
        public void teardown() {
            // Do nothing.
        }
    }
}
