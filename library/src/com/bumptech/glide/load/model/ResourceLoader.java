package com.bumptech.glide.load.model;

import android.content.Context;
import android.net.Uri;
import com.bumptech.glide.load.data.DataFetcher;

/**
 * A model loader for handling resources. Model must be a resource id in the package of the given context.
 */
public class ResourceLoader<T> implements ModelLoader<Integer, T> {

    private final ModelLoader<Uri, T> uriLoader;
    private final Context context;

    public ResourceLoader(Context context, ModelLoader<Uri, T> uriLoader) {
        this.context = context;
        this.uriLoader = uriLoader;
    }

    @Override
    public DataFetcher<T> getResourceFetcher(Integer model, int width, int height) {
        Uri uri = Uri.parse("android.resource://" + context.getPackageName() + "/" + model.toString());
        return uriLoader.getResourceFetcher(uri, width, height);
    }

    @Override
    public String getId(Integer model) {
        return model.toString();
    }
}
