package com.bumptech.glide.loader.model;

import android.content.Context;
import android.net.Uri;
import com.bumptech.glide.loader.stream.StreamLoader;

import java.io.File;

/**
 * A model loader for handling certain string models. Handles paths, urls, and any uri string with a scheme handled by
 * {@link android.content.ContentResolver#openInputStream(android.net.Uri)}.
 */
public class StringLoader implements ModelLoader<String> {

    public static class Factory implements ModelLoaderFactory<String> {

        @Override
        public ModelLoader<String> build(Context context, GenericLoaderFactory factories) {
            return new StringLoader(factories.buildModelLoader(Uri.class, context));
        }

        @Override
        public Class<? extends ModelLoader<String>> loaderClass() {
            return StringLoader.class;
        }

        @Override
        public void teardown() { }
    }

    private final ModelLoader<Uri> uriLoader;

    public StringLoader(ModelLoader<Uri> uriLoader) {
        this.uriLoader = uriLoader;
    }

    @Override
    public StreamLoader getStreamLoader(final String model, final int width, final int height) {
        Uri uri = Uri.parse(model);

        final String scheme = uri.getScheme();
        if (scheme == null) {
            uri = Uri.fromFile(new File(model));
        }

        return uriLoader.getStreamLoader(uri, width, height);
    }

    @Override
    public String getId(String model) {
        return model;
    }
}
