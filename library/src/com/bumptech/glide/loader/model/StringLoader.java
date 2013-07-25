package com.bumptech.glide.loader.model;

import android.content.Context;
import android.net.Uri;
import com.android.volley.RequestQueue;
import com.bumptech.glide.loader.stream.FileStreamLoader;
import com.bumptech.glide.loader.stream.StreamLoader;

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
    protected StreamLoader buildStreamOpener(final String model, final int width, final int height) {
        return new StreamLoader() {
            StreamLoader subStreamLoader = null;
            @Override
            public void loadStream(StreamReadyCallback cb) {
                final File file = new File(model);
                if (file.exists() && !file.isDirectory()) {
                    subStreamLoader = new FileStreamLoader(model);
                } else {
                    final Uri uri = Uri.parse(model);
                    subStreamLoader = uriLoader.buildStreamOpener(uri, width, height);
                }

                subStreamLoader.loadStream(cb);
            }

            @Override
            public void cancel() {
                if (subStreamLoader != null) {
                    subStreamLoader.cancel();
                }
            }
        };
    }

    @Override
    public String getId(String model) {
        return model;
    }
}
