package com.bumptech.glide.load.model;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;

import com.bumptech.glide.load.data.DataFetcher;

/**
 * A model loader for handling Android resource files. Model must be an Android resource id in the package of the given
 * context.
 *
 * @param <T> The type of data that will be loaded for the given android resource.
 */
public class ResourceLoader<T> implements ModelLoader<Integer, T> {

    private final ModelLoader<Uri, T> uriLoader;
    private final Resources resources;

    public ResourceLoader(Context context, ModelLoader<Uri, T> uriLoader) {
        this(context.getResources(), uriLoader);
    }

    public ResourceLoader(Resources resources, ModelLoader<Uri, T> uriLoader) {
        this.resources = resources;
        this.uriLoader = uriLoader;
    }

    @Override
    public DataFetcher<T> getResourceFetcher(Integer model, int width, int height) {
        Uri uri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                + resources.getResourcePackageName(model) + '/'
                + resources.getResourceTypeName(model) + '/'
                + resources.getResourceEntryName(model));

        return uriLoader.getResourceFetcher(uri, width, height);
    }
}
