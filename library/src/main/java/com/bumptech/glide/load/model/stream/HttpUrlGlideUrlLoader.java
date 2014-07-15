package com.bumptech.glide.load.model.stream;

import android.content.Context;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.data.HttpUrlFetcher;
import com.bumptech.glide.load.model.GenericLoaderFactory;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;

import java.io.InputStream;

/**
 * An {@link com.bumptech.glide.load.model.ModelLoader} for translating {@link com.bumptech.glide.load.model.GlideUrl}
 * (http/https URLS) into {@link java.io.InputStream} data.
 */
public class HttpUrlGlideUrlLoader implements ModelLoader<GlideUrl, InputStream> {

    /**
     * The default factory for {@link com.bumptech.glide.load.model.stream.HttpUrlGlideUrlLoader}s.
     */
    public static class Factory implements ModelLoaderFactory<GlideUrl, InputStream> {
        @Override
        public ModelLoader<GlideUrl, InputStream> build(Context context, GenericLoaderFactory factories) {
            return new HttpUrlGlideUrlLoader();
        }

        @Override
        public void teardown() {

        }
    }

    @Override
    public DataFetcher<InputStream> getResourceFetcher(GlideUrl model, int width, int height) {
        return new HttpUrlFetcher(model);
    }
}
