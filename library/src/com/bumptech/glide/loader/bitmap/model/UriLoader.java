package com.bumptech.glide.loader.bitmap.model;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import com.bumptech.glide.loader.bitmap.resource.LocalUriFetcher;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * A model loader for trying to load Uris. Capable of handling 'http', 'https', 'android.resource', 'content', and
 * 'file' schemes. Unsupported schemes will throw an exception in {@link #getResourceFetcher(Uri, int, int)}.
 */
public class UriLoader implements ModelLoader<Uri, InputStream> {

    public static class Factory implements ModelLoaderFactory<Uri, InputStream> {

        @Override
        public ModelLoader<Uri, InputStream> build(Context context, GenericLoaderFactory factories) {
            return new UriLoader(context, factories.buildModelLoader(URL.class, InputStream.class, context));
        }

        @Override
        public Class<? extends ModelLoader<Uri, InputStream>> loaderClass() {
            return UriLoader.class;
        }

        @Override
        public void teardown() { }
    }

    private final Context context;
    private final ModelLoader<URL, InputStream> urlLoader;

    public UriLoader(Context context, ModelLoader<URL, InputStream> urlLoader) {
        this.context = context;
        this.urlLoader = urlLoader;
    }

    @Override
    public ResourceFetcher<InputStream> getResourceFetcher(Uri model, int width, int height) {
        final String scheme = model.getScheme();

        ResourceFetcher<InputStream> result = null;
        if (isLocalUri(scheme)) {
            result = new LocalUriFetcher(context, model);
        } else if ("http".equals(scheme) || "https".equals(scheme)) {
            try {
                result = urlLoader.getResourceFetcher(new URL(model.toString()), width, height);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        if (result == null) {
            throw new IllegalArgumentException("No stream loader for uri=" + model);
        }

        return result;
    }


    @Override
    public String getId(Uri model) {
        return model.toString();
    }

    private boolean isLocalUri(String scheme) {
        return ContentResolver.SCHEME_FILE.equals(scheme)
                || ContentResolver.SCHEME_CONTENT.equals(scheme)
                || ContentResolver.SCHEME_ANDROID_RESOURCE.equals(scheme);
    }
}
