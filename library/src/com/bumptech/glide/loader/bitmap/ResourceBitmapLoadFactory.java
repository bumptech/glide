package com.bumptech.glide.loader.bitmap;

import com.bumptech.glide.loader.bitmap.model.ModelLoader;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;
import com.bumptech.glide.resize.load.BitmapLoad;
import com.bumptech.glide.resize.load.BitmapDecoder;
import com.bumptech.glide.resize.load.ResourceBitmapLoad;
import com.bumptech.glide.resize.load.Transformation;

public class ResourceBitmapLoadFactory<T, Y> implements BitmapLoadFactory<T> {
    private final ModelLoader<T, Y> modelLoader;
    private final BitmapDecoder<Y> decoder;

    public ResourceBitmapLoadFactory(ModelLoader<T, Y> modelLoader, BitmapDecoder<Y> decoder) {
        if (modelLoader == null) {
            throw new IllegalArgumentException("Can't create load factory with null model loader");
        }
        if (decoder == null) {
            throw new IllegalArgumentException("Can't create load factory with null decoder");
        }
        this.modelLoader = modelLoader;
        this.decoder = decoder;
    }

    @Override
    public BitmapLoad getLoadTask(T model, int width, int height) {
        final String id = modelLoader.getId(model);
        final ResourceFetcher<Y> resourceFetcher = modelLoader.getResourceFetcher(model, width, height);
        return new ResourceBitmapLoad<Y>(id, resourceFetcher, decoder, width, height, Transformation.NONE);
    }
}
