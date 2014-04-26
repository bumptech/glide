package com.bumptech.glide;

import android.content.Context;
import android.os.ParcelFileDescriptor;
import com.bumptech.glide.loader.bitmap.model.ModelLoaderFactory;
import com.bumptech.glide.resize.load.BitmapDecoder;
import com.bumptech.glide.resize.load.Downsampler;
import com.bumptech.glide.resize.load.Transformation;
import com.bumptech.glide.resize.load.VideoBitmapDecoder;

import java.io.InputStream;

/**
 * A class for creating a request to load a bitmap for an image or from a video. Sets a variety of type independent
 * options including resizing, animations, and placeholders.
 *
 * @param <ModelType> The type of model that will be loaded into the target.
 */
@SuppressWarnings("unused") //public api
public class RequestBuilder<ModelType> extends GenericRequestBuilder<ModelType, InputStream, ParcelFileDescriptor> {

    RequestBuilder(Context context, ModelType model) {
        this(context, model, Glide.get(context).getFactory(model, InputStream.class),
                Glide.get(context).getFactory(model, ParcelFileDescriptor.class));
    }

    RequestBuilder(Context context, ModelType model, ModelLoaderFactory<ModelType, InputStream> imageFactory,
            ModelLoaderFactory<ModelType, ParcelFileDescriptor> videoFactory) {
        super(context, model, imageFactory, videoFactory);

        approximate().videoDecoder(new VideoBitmapDecoder());
    }

    /**
     * Load images at a size near the size of the target using {@link Downsampler#AT_LEAST}.
     *
     * @see #downsample(com.bumptech.glide.resize.load.Downsampler)
     *
     * @return This RequestBuilder
     */
    public RequestBuilder<ModelType> approximate() {
        return downsample(Downsampler.AT_LEAST);
    }

    /**
     * Load images at their original size using {@link Downsampler#NONE}.
     *
     * @see #downsample(com.bumptech.glide.resize.load.Downsampler)
     *
     * @return This RequestBuilder
     */
    public RequestBuilder<ModelType> asIs() {
        return downsample(Downsampler.NONE);
    }

    /**
     * Load images using the given {@link Downsampler}. Replaces any existing image decoder. Defaults to
     * {@link Downsampler#AT_LEAST}. Will be ignored if the data represented by the model is a video.
     *
     * @see #imageDecoder
     * @see #videoDecoder(BitmapDecoder)
     *
     * @param downsampler The downsampler
     * @return This RequestBuilder
     */
    public RequestBuilder<ModelType> downsample(Downsampler downsampler) {
        super.imageDecoder(downsampler);
        return this;
    }

    public RequestBuilder<ModelType> thumbnail(float sizeMultiplier) {
        super.thumbnail(sizeMultiplier);
        return this;
    }

    public RequestBuilder<ModelType> thumbnail(RequestBuilder<ModelType> thumbnailRequest) {
        super.thumbnail(thumbnailRequest);
        return this;
    }

     public RequestBuilder<ModelType> sizeMultiplier(float sizeMultiplier) {
         super.sizeMultiplier(sizeMultiplier);
         return this;
     }

    @Override
    public RequestBuilder<ModelType> imageDecoder(BitmapDecoder<InputStream> decoder) {
        super.imageDecoder(decoder);
        return this;
    }

    @Override
    public RequestBuilder<ModelType> videoDecoder(BitmapDecoder<ParcelFileDescriptor> decoder) {
        super.videoDecoder(decoder);
        return this;
    }

    @Override
    public RequestBuilder<ModelType> centerCrop() {
        super.centerCrop();
        return this;
    }

    @Override
    public RequestBuilder<ModelType> fitCenter() {
        super.fitCenter();
        return this;
    }

    @Override
    public RequestBuilder<ModelType> transform(Transformation transformation) {
        super.transform(transformation);
        return this;
    }

    @Override
    public RequestBuilder<ModelType> animate(int animationId) {
        super.animate(animationId);
        return this;
    }

    @Override
    public RequestBuilder<ModelType> placeholder(int resourceId) {
        super.placeholder(resourceId);
        return this;
    }

    @Override
    public RequestBuilder<ModelType> error(int resourceId) {
        super.error(resourceId);
        return this;
    }

    @Override
    public RequestBuilder<ModelType> listener(RequestListener<ModelType> requestListener) {
        super.listener(requestListener);
        return this;
    }
}
