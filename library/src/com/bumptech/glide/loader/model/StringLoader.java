package com.bumptech.glide.loader.model;

import android.content.Context;
import android.net.Uri;
import com.android.volley.RequestQueue;
import com.bumptech.glide.loader.stream.FileStreamLoader;
import com.bumptech.glide.loader.stream.StreamLoader;
import com.bumptech.glide.util.Log;

import java.io.File;

/**
 * A model loader for handling certain string models. Handles paths, urls, and any uri string with a scheme handled by
 * {@link android.content.ContentResolver#openInputStream(android.net.Uri)}.
 */
public class StringLoader extends BaseModelLoader<String> {

    private final UriLoader uriLoader;

    public StringLoader(Context context, RequestQueue requestQueue) {
        uriLoader = new UriLoader(context, requestQueue);
    }

    @Override
    protected StreamLoader buildStreamOpener(String model, int width, int height) {
        final File file = new File(model);
        if (file.exists() && !file.isDirectory()) {
            Log.d("TEST: string loader have file exists and not dir");
            return new FileStreamLoader(model);
        } else {
            final Uri uri = Uri.parse(model);
            Log.d("TEST: string loader parsed uri to: " + uri);
            return uriLoader.buildStreamOpener(uri, width, height);
        }
    }

    @Override
    public String getId(String model) {
        return model;
    }
}
