package com.bumptech.glide.loader.bitmap.model;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;

import java.net.MalformedURLException;
import java.net.URL;

public abstract class UriLoader<T> implements ModelLoader<Uri, T>{
    private final Context context;
    private final ModelLoader<URL, T> urlLoader;

    public UriLoader(Context context, ModelLoader<URL, T> urlLoader) {
        this.context = context;
        this.urlLoader = urlLoader;
    }

    @Override
    public final ResourceFetcher<T> getResourceFetcher(Uri model, int width, int height) {
        final String scheme = model.getScheme();

        ResourceFetcher<T> result = null;
        if (isLocalUri(scheme)) {
            result = getLocalUriFetcher(context, model);
        } else if ("http".equals(scheme) || "https".equals(scheme)) {
            try {
                result = urlLoader.getResourceFetcher(new URL(model.toString()), width, height);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        if (result == null) {
            throw new IllegalArgumentException("No stream loader for uri=" + model);
        }

        return result;
    }

    @Override
    public final String getId(Uri model) {
        return model.toString();
    }

    protected abstract ResourceFetcher<T> getLocalUriFetcher(Context context, Uri uri);

    private boolean isLocalUri(String scheme) {
        return ContentResolver.SCHEME_FILE.equals(scheme)
                || ContentResolver.SCHEME_CONTENT.equals(scheme)
                || ContentResolver.SCHEME_ANDROID_RESOURCE.equals(scheme);
    }
}
