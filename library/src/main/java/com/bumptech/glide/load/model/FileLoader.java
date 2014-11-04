package com.bumptech.glide.load.model;

import android.net.Uri;

import com.bumptech.glide.load.data.DataFetcher;

import java.io.File;

/**
 * A simple model loader for loading data from {@link File}s.
 *
 * @param <T> The type of data loaded from the given {@link java.io.File} ({@link java.io.InputStream} or
 *           {@link java.io.FileDescriptor} etc).
 */
public class FileLoader<T> implements ModelLoader<File, T> {

    private final ModelLoader<Uri, T> uriLoader;

    public FileLoader(ModelLoader<Uri, T> uriLoader) {
        this.uriLoader = uriLoader;
    }

    @Override
    public DataFetcher<T> getResourceFetcher(File model, int width, int height) {
        return uriLoader.getResourceFetcher(Uri.fromFile(model), width, height);
    }
}
