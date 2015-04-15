package com.bumptech.glide.load.model;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;

import com.bumptech.glide.load.data.DataFetcher;

/**
 * A model loader for handling Android resource files. Model must be an Android resource id in the package of the given
 * context.
 *
 * @param <T> The type of data that will be loaded for the given android resource.
 */
public class ResourceLoader<T> implements ModelLoader<Integer, T> {
    private static final String TAG = "ResourceLoader";

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
        Uri uri = null;
        try {
          uri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                  + resources.getResourcePackageName(model) + '/'
                  + resources.getResourceTypeName(model) + '/'
                  + resources.getResourceEntryName(model));
        } catch (Resources.NotFoundException e) {
            if (Log.isLoggable(TAG, Log.WARN)) {
                Log.w(TAG, "Received invalid resource id: " + model, e);
            }
        }

        if (uri != null) {
            return uriLoader.getResourceFetcher(uri, width, height);
        } else {
            return null;
        }
    }
}
