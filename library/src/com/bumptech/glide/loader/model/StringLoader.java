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

    private final ModelLoader<Uri> uriLoader;

    /**
     * A convenience constructor that defaults to {@link UriLoader} for loading uri strings
     *
     * @param context A context
     */
    public StringLoader(Context context) {
        this(new UriLoader(context));
    }

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
