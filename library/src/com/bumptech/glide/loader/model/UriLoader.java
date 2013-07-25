package com.bumptech.glide.loader.model;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import com.android.volley.RequestQueue;
import com.bumptech.glide.loader.stream.LocalUriLoader;
import com.bumptech.glide.loader.stream.StreamLoader;
import com.bumptech.glide.loader.stream.VolleyStreamLoader;

/**
 * A model loader for trying to load Uris. Capable of handling 'http', 'android.resource', 'content', and 'file'
 * schemes. Unsupported schemes will throw an exception in {@link #buildStreamOpener(android.net.Uri, int, int)}.
 */
public class UriLoader extends BaseModelLoader<Uri> {
    private final Context context;
    private final RequestQueue requestQueue;

    public UriLoader(Context context, RequestQueue requestQueue) {
        this.context = context;
        this.requestQueue = requestQueue;
    }

    @Override
    protected StreamLoader buildStreamOpener(Uri model, int width, int height) {
        final String scheme = model.getScheme();

        StreamLoader result = null;
        if (isLocalUri(scheme)) {
            result = new LocalUriLoader(context, model);
        } else if ("http".equals(scheme)) {
            result = new VolleyStreamLoader(requestQueue, model.toString());
        }

        if (result == null) {
            throw new IllegalArgumentException("No stream loader for uri=" + model);
        }

        return result;
    }

    @Override
    public String getId(Uri model) {
        return model.toString();
    }

    private boolean isLocalUri(String scheme) {
        return ContentResolver.SCHEME_FILE.equals(scheme)
                || ContentResolver.SCHEME_CONTENT.equals(scheme)
                || ContentResolver.SCHEME_ANDROID_RESOURCE.equals(scheme);
    }
}
