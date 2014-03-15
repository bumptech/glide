package com.bumptech.glide.loader.bitmap.model;

import android.content.Context;
import android.net.Uri;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;

import java.io.File;
import java.io.InputStream;

/**
 * A model loader for handling certain string models. Handles paths, urls, and any uri string with a scheme handled by
 * {@link android.content.ContentResolver#openInputStream(android.net.Uri)}.
 */
public class StringLoader implements ModelLoader<String, InputStream> {

    public static class Factory implements ModelLoaderFactory<String, InputStream> {
        @Override
        public ModelLoader<String, InputStream> build(Context context, GenericLoaderFactory factories) {
            return new StringLoader(factories.buildModelLoader(Uri.class, InputStream.class, context));
        }

        @Override
        public Class<? extends ModelLoader<String, InputStream>> loaderClass() {
            return StringLoader.class;
        }

        @Override
        public void teardown() { }
    }

    private final ModelLoader<Uri, InputStream> uriLoader;

    public StringLoader(ModelLoader<Uri, InputStream> uriLoader) {
        this.uriLoader = uriLoader;
    }

    @Override
    public ResourceFetcher<InputStream> getResourceFetcher(String model, int width, int height) {
        Uri uri = Uri.parse(model);

        final String scheme = uri.getScheme();
        if (scheme == null) {
            uri = Uri.fromFile(new File(model));
        }
        return uriLoader.getResourceFetcher(uri, width, height);
    }

    @Override
    public String getId(String model) {
        return model;
    }
}
