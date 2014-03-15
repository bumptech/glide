package com.bumptech.glide.loader.bitmap;

import com.bumptech.glide.loader.bitmap.model.ModelLoader;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;
import com.bumptech.glide.loader.bitmap.transformation.None;
import com.bumptech.glide.loader.bitmap.transformation.TransformationLoader;
import com.bumptech.glide.resize.BaseBitmapLoadTask;
import com.bumptech.glide.resize.BitmapLoadTask;
import com.bumptech.glide.resize.load.BitmapDecoder;
import com.bumptech.glide.resize.load.Transformation;

/**
 * A base {@link BitmapLoadFactory} that composes {@link ModelLoader} and {@link BitmapDecoder} sub-components
 * to create an {@link BitmapLoadTask}.
 *
 * @param <T> The type of the model.
 * @param <Y> The type of the resource that the {@link ModelLoader} provides and the {@link BitmapDecoder} can
 *           decode.`
 */
public class BaseBitmapLoadFactory<T, Y> implements BitmapLoadFactory<T> {
    private final ModelLoader<T, Y> modelLoader;
    private final BitmapDecoder<Y> decoder;
    private final TransformationLoader<T> transformationLoader;

    public BaseBitmapLoadFactory(ModelLoader<T, Y> modelLoader, BitmapDecoder<Y> decoder) {
        this(modelLoader, decoder, new None<T>());
    }

    public BaseBitmapLoadFactory(ModelLoader<T, Y> modelLoader, BitmapDecoder<Y> decoder,
            TransformationLoader<T> transformationLoader) {
        this.modelLoader = modelLoader;
        this.decoder = decoder;
        this.transformationLoader = transformationLoader;
    }

    @Override
    public BitmapLoadTask getLoadTask(T model, int width, int height) {
        ResourceFetcher<Y> resourceFetcher = modelLoader.getResourceFetcher(model, width, height);
        Transformation transformation = transformationLoader.getTransformation(model);
        return new BaseBitmapLoadTask<Y>(resourceFetcher, decoder, transformation, width, height);
    }
}
