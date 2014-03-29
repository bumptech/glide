package com.bumptech.glide.loader.bitmap;

import com.bumptech.glide.loader.bitmap.model.ModelLoader;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;
import com.bumptech.glide.loader.bitmap.transformation.None;
import com.bumptech.glide.loader.bitmap.transformation.TransformationLoader;
import com.bumptech.glide.resize.BitmapLoad;
import com.bumptech.glide.resize.load.ImageVideoBitmapLoad;
import com.bumptech.glide.resize.load.BitmapDecoder;
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
    private final ModelLoader<T, Y> imageModelLoader;
    private final BitmapDecoder<Y> imageDecoder;
    private final ModelLoader<T, Z> videoModelLoader;
    private final BitmapDecoder<Z> videoDecoder;
    private final TransformationLoader<T> transformationLoader;

    @SuppressWarnings("unused")
    public ImageVideoBitmapLoadFactory(ModelLoader<T, Y> imageModelLoader, BitmapDecoder<Y> imageDecoder) {
        this(imageModelLoader, imageDecoder, null, null, new None<T>());
    }

    @SuppressWarnings("unused")
    public ImageVideoBitmapLoadFactory(ModelLoader<T, Y> imageModelLoader, BitmapDecoder<Y> imageDecoder,
            TransformationLoader<T> transformationLoader) {
        this(imageModelLoader, imageDecoder, null, null, transformationLoader);
    }

    public ImageVideoBitmapLoadFactory(ModelLoader<T, Y> imageModelLoader, BitmapDecoder<Y> imageDecoder,
            ModelLoader<T, Z> videoModelLoader, BitmapDecoder<Z> videoDecoder,
            TransformationLoader<T> transformationLoader) {
        if ((imageModelLoader == null || imageDecoder == null)
                && (videoModelLoader == null || videoDecoder == null)) {
            throw new IllegalArgumentException("You must provide at least a video model loader and a video decoder or" +
                    "an image model loader and an image decoder");
        }
        if (transformationLoader == null) {
            throw new IllegalArgumentException("You must provide a non null transformation loader");
        }
        this.imageModelLoader = imageModelLoader;
        this.imageDecoder = imageDecoder;
        this.videoModelLoader = videoModelLoader;
        this.videoDecoder = videoDecoder;
        this.transformationLoader = transformationLoader;
    }

    @Override
    public BitmapLoad getLoadTask(T model, int width, int height) {
        String imageId = null;
        ResourceFetcher<Y> imageFetcher = null;
        if (imageModelLoader != null) {
            imageId = imageModelLoader.getId(model);
            imageFetcher = imageModelLoader.getResourceFetcher(model, width, height);
        }
        String videoId = null;
        ResourceFetcher<Z> videoFetcher = null;
        if (videoModelLoader != null) {
            videoId = videoModelLoader.getId(model);
            videoFetcher = videoModelLoader.getResourceFetcher(model, width, height);
        }
        Transformation transformation = transformationLoader.getTransformation(model);
        return new ImageVideoBitmapLoad<Y, Z>(imageId, imageFetcher, imageDecoder, videoId, videoFetcher, videoDecoder,
                transformation, width, height);
    }
}
