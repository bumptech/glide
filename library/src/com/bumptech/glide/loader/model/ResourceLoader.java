package com.bumptech.glide.loader.model;

import android.content.Context;
import android.net.Uri;
import com.bumptech.glide.loader.stream.LocalUriLoader;
import com.bumptech.glide.loader.stream.StreamLoader;

/**
 * A model loader for handling resources. Model must be a resource id in the package of
 * the given context.
 */
public class ResourceLoader implements ModelLoader<Integer> {
    private final Context context;

    public ResourceLoader(Context context) {
        this.context = context;
    }

    @Override
    public StreamLoader getStreamLoader(Integer model, int width, int height) {
        Uri uri = Uri.parse("android.resource://" + context.getPackageName() + "/" + model.toString());
        return new LocalUriLoader(context, uri);
    }

    @Override
    public String getId(Integer model) {
        return model.toString();
    }
}
