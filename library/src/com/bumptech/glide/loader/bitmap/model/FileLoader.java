package com.bumptech.glide.loader.bitmap.model;

import android.content.Context;
import android.net.Uri;
import com.bumptech.glide.loader.bitmap.model.stream.StreamModelLoader;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;

import java.io.File;
import java.io.InputStream;

/**
 * A simple model loader for {@link File}
 */
public class FileLoader<T> implements ModelLoader<File, T> {

    private final ModelLoader<Uri, T> uriLoader;

    public FileLoader(ModelLoader<Uri, T> uriLoader) {
        this.uriLoader = uriLoader;
    }

    @Override
    public ResourceFetcher<T> getResourceFetcher(File model, int width, int height) {
        return uriLoader.getResourceFetcher(Uri.fromFile(model), width, height);
    }

    @Override
    public String getId(File model) {
        return model.getAbsolutePath();
    }
}
