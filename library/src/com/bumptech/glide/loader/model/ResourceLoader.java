package com.bumptech.glide.loader.model;

import android.content.Context;
import android.net.Uri;
import com.bumptech.glide.loader.stream.StreamLoader;

/**
 * A model loader for handling resources. Model must be a resource id in the package of
 * the given context.
 */
public class ResourceLoader implements ModelLoader<Integer> {

    public static class Factory implements ModelLoaderFactory<Integer> {

        @Override
        public ModelLoader<Integer> build(Context context, GenericLoaderFactory factories) {
            return new ResourceLoader(context, factories.buildModelLoader(Uri.class, context));
        }

        @Override
        public Class<? extends ModelLoader<Integer>> loaderClass() {
            return ResourceLoader.class;
        }

        @Override
        public void teardown() { }
    }

    private final ModelLoader<Uri> uriLoader;
    private final Context context;

    public ResourceLoader(Context context, ModelLoader<Uri> uriLoader) {
        this.context = context;
        this.uriLoader = uriLoader;
    }

    @Override
    public StreamLoader getStreamLoader(Integer model, int width, int height) {
        Uri uri = Uri.parse("android.resource://" + context.getPackageName() + "/" + model.toString());
        return uriLoader.getStreamLoader(uri, width, height);
    }

    @Override
    public String getId(Integer model) {
        return model.toString();
    }
}
