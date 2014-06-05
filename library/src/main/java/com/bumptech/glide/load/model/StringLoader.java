package com.bumptech.glide.load.model;

import android.net.Uri;
import com.bumptech.glide.load.data.DataFetcher;

import java.io.File;

/**
 * A model loader for handling certain string models. Handles paths, urls, and any uri string with a scheme handled by
 * {@link android.content.ContentResolver#openInputStream(Uri)}.
 */
public class StringLoader<T> implements ModelLoader<String, T> {
    private final ModelLoader<Uri, T> uriLoader;

    public StringLoader(ModelLoader<Uri, T> uriLoader) {
        this.uriLoader = uriLoader;
    }

    @Override
    public DataFetcher<T> getResourceFetcher(String model, int width, int height) {
        Uri uri = Uri.parse(model);

        final String scheme = uri.getScheme();
        if (scheme == null) {
            uri = Uri.fromFile(new File(model));
        }
        return uriLoader.getResourceFetcher(uri, width, height);
    }

    @Override
    public final String getId(String model) {
        return model;
    }
}
