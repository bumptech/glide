package com.bumptech.glide.load.model;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.data.FileDescriptorLocalUriFetcher;
import com.bumptech.glide.load.data.StreamLocalUriFetcher;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A base ModelLoader for {@link android.net.Uri}s that handles local {@link android.net.Uri}s directly and routes
 * remote {@link android.net.Uri}s to a wrapped {@link com.bumptech.glide.load.model.ModelLoader} that handles
 * {@link com.bumptech.glide.load.model.GlideUrl}s.
 *
 * @param <Data> The type of data that will be retrieved for {@link android.net.Uri}s.
 */
public class UriLoader<Data> implements ModelLoader<Uri, Data> {
    private static final Set<String> SCHEMES = Collections.unmodifiableSet(
            new HashSet<String>(
                    Arrays.asList(
                            ContentResolver.SCHEME_FILE,
                            ContentResolver.SCHEME_ANDROID_RESOURCE,
                            ContentResolver.SCHEME_CONTENT
                    )
            )
    );

    private final Context context;
    private final LocalUriFetcherFactory<Data> factory;

    public UriLoader(Context context, LocalUriFetcherFactory<Data> factory) {
        this.context = context;
        this.factory = factory;
    }

    @Override
    public final DataFetcher<Data> getDataFetcher(Uri model, int width, int height) {
        return factory.build(context, model);
    }

    @Override
    public boolean handles(Uri model) {
        return SCHEMES.contains(model.getScheme());
    }

    public interface LocalUriFetcherFactory<Data> {
        DataFetcher<Data> build(Context context, Uri uri);
    }

    public static class StreamFactory implements ModelLoaderFactory<Uri, InputStream>,
            LocalUriFetcherFactory<InputStream> {

        @Override
        public DataFetcher<InputStream> build(Context context, Uri uri) {
            return new StreamLocalUriFetcher(context, uri);
        }

        @Override
        public ModelLoader<Uri, InputStream> build(Context context, MultiModelLoaderFactory multiFactory) {
            return new UriLoader<InputStream>(context, this);
        }

        @Override
        public void teardown() {
            // Do nothing.
        }
    }

    public static class FileDescriptorFactory implements ModelLoaderFactory<Uri, ParcelFileDescriptor>,
            LocalUriFetcherFactory<ParcelFileDescriptor> {

        @Override
        public DataFetcher<ParcelFileDescriptor> build(Context context, Uri uri) {
            return new FileDescriptorLocalUriFetcher(context, uri);
        }

        @Override
        public ModelLoader<Uri, ParcelFileDescriptor> build(Context context, MultiModelLoaderFactory multiFactory) {
            return new UriLoader<ParcelFileDescriptor>(context, this);
        }

        @Override
        public void teardown() {
            // Do nothing.
        }
    }
}
