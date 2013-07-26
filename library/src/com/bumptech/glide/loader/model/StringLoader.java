package com.bumptech.glide.loader.model;

import android.content.Context;
import android.net.Uri;
import android.webkit.URLUtil;
import com.bumptech.glide.loader.stream.StreamLoader;

import java.io.File;
import java.net.URL;

/**
 * A model loader for handling certain string models. Handles paths, urls, and any uri string with a scheme handled by
 * {@link android.content.ContentResolver#openInputStream(android.net.Uri)}.
 */
public class StringLoader extends BaseModelLoader<String> {

    private final UriLoader uriLoader;

    public StringLoader(Context context, ModelLoader<URL> urlLoader) {
        uriLoader = new UriLoader(context, urlLoader);
    }

    @Override
    protected StreamLoader buildStreamOpener(final String model, final int width, final int height) {
        final Uri uri;
        if (!URLUtil.isValidUrl(model)) {
            uri = Uri.fromFile(new File(model));
        } else {
            uri = Uri.parse(model);
        }

        return uriLoader.buildStreamOpener(uri, width, height);
    }

    @Override
    public String getId(String model) {
        return model;
    }
}
