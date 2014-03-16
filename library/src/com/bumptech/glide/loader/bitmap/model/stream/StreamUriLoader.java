package com.bumptech.glide.loader.bitmap.model.stream;

import android.content.Context;
import android.net.Uri;
import com.bumptech.glide.loader.bitmap.model.GenericLoaderFactory;
import com.bumptech.glide.loader.bitmap.model.ModelLoader;
import com.bumptech.glide.loader.bitmap.model.ModelLoaderFactory;
import com.bumptech.glide.loader.bitmap.model.UriLoader;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;
import com.bumptech.glide.loader.bitmap.resource.StreamLocalUriFetcher;

import java.io.InputStream;
import java.net.URL;

/**
 * A {@link ModelLoader} for translating uri models into {@link InputStream} resources. Capable of handling 'http',
 * 'https', 'android.resource', 'content', and 'file' schemes. Unsupported schemes will throw an exception in
 * {@link #getResourceFetcher(Uri, int, int)}.
 */
public class StreamUriLoader extends UriLoader<InputStream> implements StreamModelLoader<Uri> {

    public static class Factory implements ModelLoaderFactory<Uri, InputStream> {

        @Override
        public ModelLoader<Uri, InputStream> build(Context context, GenericLoaderFactory factories) {
            return new StreamUriLoader(context, factories.buildModelLoader(URL.class, InputStream.class, context));
        }

        @Override
        public Class<? extends ModelLoader<Uri, InputStream>> loaderClass() {
            return StreamUriLoader.class;
        }

        @Override
        public void teardown() { }
    }

    public StreamUriLoader(Context context, ModelLoader<URL, InputStream> urlLoader) {
        super(context, urlLoader);
    }

    @Override
    protected ResourceFetcher<InputStream> getLocalUriFetcher(Context context, Uri uri) {
        return new StreamLocalUriFetcher(context, uri);
    }
}
