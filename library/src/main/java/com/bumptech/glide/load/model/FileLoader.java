package com.bumptech.glide.load.model;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.bumptech.glide.load.data.DataFetcher;

import java.io.File;
import java.io.InputStream;

/**
 * A simple model loader for loading data from {@link File}s.
 *
 * @param <Data> The type of data loaded from the given {@link java.io.File} ({@link java.io.InputStream} or
 *           {@link java.io.FileDescriptor} etc).
 */
public class FileLoader<Data> implements ModelLoader<File, Data> {

    private final ModelLoader<Uri, Data> uriLoader;

    public FileLoader(ModelLoader<Uri, Data> uriLoader) {
        this.uriLoader = uriLoader;
    }

    @Override
    public DataFetcher<Data> getDataFetcher(File model, int width, int height) {
        return uriLoader.getDataFetcher(Uri.fromFile(model), width, height);
    }

    @Override
    public boolean handles(File model) {
        return true;
    }

    /**
     * Factory for loading {@link InputStream}s from {@link File}s.
     */
    public static class StreamFactory implements ModelLoaderFactory<File, InputStream> {

        @Override
        public ModelLoader<File, InputStream> build(Context context, MultiModelLoaderFactory multiFactory) {
            return new FileLoader<InputStream>(multiFactory.build(Uri.class, InputStream.class));
        }

        @Override
        public void teardown() {
            // Do nothing.
        }
    }

    /**
     * Factory for loading {@link ParcelFileDescriptor}s from {@link File}s.
     */
    public static class FileDescriptorFactory implements ModelLoaderFactory<File, ParcelFileDescriptor> {

        @Override
        public ModelLoader<File, ParcelFileDescriptor> build(Context context, MultiModelLoaderFactory multiFactory) {
            return new FileLoader<ParcelFileDescriptor>(multiFactory.build(Uri.class, ParcelFileDescriptor.class));
        }

        @Override
        public void teardown() {
            // Do nothing.
        }
    }
}
