package com.bumptech.glide.load.model;

import android.net.Uri;
import com.bumptech.glide.load.data.DataFetcher;

import java.io.File;

/**
 * A simple model loader for {@link File}
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

    @Override
    public String getId(File model) {
        return model.getAbsolutePath();
    }
}
