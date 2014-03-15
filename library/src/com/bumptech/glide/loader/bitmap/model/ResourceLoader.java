package com.bumptech.glide.loader.bitmap.model;

import android.content.Context;
import android.net.Uri;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;

import java.io.InputStream;

/**
 * A model loader for handling resources. Model must be a resource id in the package of the given context.
 */
public class ResourceLoader implements ModelLoader<Integer, InputStream> {

    public static class Factory implements ModelLoaderFactory<Integer, InputStream> {

        @Override
        public ModelLoader<Integer, InputStream> build(Context context, GenericLoaderFactory factories) {
            return new ResourceLoader(context, factories.buildModelLoader(Uri.class, InputStream.class, context));
        }

        @Override
        public Class<? extends ModelLoader<Integer, InputStream>> loaderClass() {
            return ResourceLoader.class;
        }

        @Override
        public void teardown() { }
    }

    private final ModelLoader<Uri, InputStream> uriLoader;
    private final Context context;

    public ResourceLoader(Context context, ModelLoader<Uri, InputStream> uriLoader) {
        this.context = context;
        this.uriLoader = uriLoader;
    }

    @Override
    public ResourceFetcher<InputStream> getResourceFetcher(Integer model, int width, int height) {
        Uri uri = Uri.parse("android.resource://" + context.getPackageName() + "/" + model.toString());
        return uriLoader.getResourceFetcher(uri, width, height);
    }

    @Override
    public String getId(Integer model) {
        return model.toString();
    }
}
