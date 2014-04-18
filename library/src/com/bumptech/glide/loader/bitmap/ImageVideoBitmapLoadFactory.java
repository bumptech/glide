package com.bumptech.glide.loader.bitmap;

import com.bumptech.glide.loader.bitmap.model.ModelLoader;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;
import com.bumptech.glide.loader.bitmap.transformation.None;
import com.bumptech.glide.loader.bitmap.transformation.TransformationLoader;
import com.bumptech.glide.resize.BitmapLoad;
import com.bumptech.glide.resize.load.ImageVideoBitmapLoad;
import com.bumptech.glide.resize.load.BitmapDecoder;
import com.bumptech.glide.resize.load.ResourceBitmapLoad;
import com.bumptech.glide.resize.load.Transformation;

/**
 * A base {@link BitmapLoadFactory} that composes {@link ModelLoader} and {@link BitmapDecoder} sub-components
 * to create an {@link BitmapLoad} capable of loading a model that represents either an image or a video.
 *
 * @param <T> The type of the model.
 * @param <Y> The type of the resource that the image {@link ModelLoader} provides and the image {@link BitmapDecoder} can
 *           decode.`
 * @param <Z> The type of resource that the video {@link ModelLoader} provides and the video {@link BitmapDecoder} can
 *           decode.
 */
public class ImageVideoBitmapLoadFactory<T, Y, Z> implements BitmapLoadFactory<T> {
    private final TransformationLoader<T> transformationLoader;
    private final ResourceBitmapLoadFactory<T, Y> imageLoadFactory;
    private final ResourceBitmapLoadFactory<T, Z> videoLoadFactory;

    public ImageVideoBitmapLoadFactory(ResourceBitmapLoadFactory<T, Y> imageLoadFactory,
            ResourceBitmapLoadFactory<T, Z> videoLoadFactory, TransformationLoader<T> transformationLoader) {
        this.imageLoadFactory = imageLoadFactory;
        this.videoLoadFactory = videoLoadFactory;
        if (imageLoadFactory == null && videoLoadFactory == null) {
            throw new IllegalArgumentException("You must provide at least a video model loader and a video decoder or" +
                    "an image model loader and an image decoder");
        }
        if (transformationLoader == null) {
            throw new IllegalArgumentException("You must provide a non null transformation loader");
        }
        this.transformationLoader = transformationLoader;
    }

    @Override
    public BitmapLoad getLoadTask(T model, int width, int height) {
        BitmapLoad imageLoad = null;
        if (imageLoadFactory != null) {
            imageLoad = imageLoadFactory.getLoadTask(model, width, height);
        }
        BitmapLoad videoLoad = null;
        if (videoLoadFactory != null) {
            videoLoad = videoLoadFactory.getLoadTask(model, width, height);
        }
        Transformation transformation = transformationLoader.getTransformation(model);
        return new ImageVideoBitmapLoad(imageLoad, videoLoad, width, height, transformation);
    }
}
