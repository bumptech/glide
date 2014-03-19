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
 * to create an {@link BitmapLoadTask} capable of loading a model that represents either an image or a video.
 *
 * @param <T> The type of the model.
 * @param <Y> The type of the resource that the image {@link ModelLoader} provides and the image {@link BitmapDecoder} can
 *           decode.`
 * @param <Z> The type of resource that the video {@link ModelLoader} provides and the video {@link BitmapDecoder} can
 *           decode.
 */
public class BaseBitmapLoadFactory<T, Y, Z> implements BitmapLoadFactory<T> {
    private final ModelLoader<T, Y> imageModelLoader;
    private final BitmapDecoder<Y> imageDecoder;
    private final ModelLoader<T, Z> videoModelLoader;
    private final BitmapDecoder<Z> videoDecoder;
    private final TransformationLoader<T> transformationLoader;

    @SuppressWarnings("unused")
    public BaseBitmapLoadFactory(ModelLoader<T, Y> imageModelLoader, BitmapDecoder<Y> imageDecoder) {
        this(imageModelLoader, imageDecoder, null, null, new None<T>());
    }

    @SuppressWarnings("unused")
    public BaseBitmapLoadFactory(ModelLoader<T, Y> imageModelLoader, BitmapDecoder<Y> imageDecoder,
            TransformationLoader<T> transformationLoader) {
        this(imageModelLoader, imageDecoder, null, null, transformationLoader);
    }

    public BaseBitmapLoadFactory(ModelLoader<T, Y> imageModelLoader, BitmapDecoder<Y> imageDecoder,
            ModelLoader<T, Z> videoModelLoader, BitmapDecoder<Z> videoDecoder,
            TransformationLoader<T> transformationLoader) {
        this.imageModelLoader = imageModelLoader;
        this.imageDecoder = imageDecoder;
        this.videoModelLoader = videoModelLoader;
        this.videoDecoder = videoDecoder;
        this.transformationLoader = transformationLoader;
    }

    @Override
    public BitmapLoadTask getLoadTask(T model, int width, int height) {
        ResourceFetcher<Y> imageFetcher = null;
        if (imageModelLoader != null) {
            imageFetcher = imageModelLoader.getResourceFetcher(model, width, height);
        }
        ResourceFetcher<Z> videoFetcher = null;
        if (videoModelLoader != null) {
            videoFetcher = videoModelLoader.getResourceFetcher(model, width, height);
        }
        Transformation transformation = transformationLoader.getTransformation(model);
        return new BaseBitmapLoadTask<Y, Z>(imageFetcher, imageDecoder, videoFetcher, videoDecoder, transformation,
                width, height);
    }
}
