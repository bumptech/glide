package com.bumptech.glide;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.ParcelFileDescriptor;
import android.view.animation.Animation;
import com.bumptech.glide.resize.Priority;
import com.bumptech.glide.resize.RequestContext;
import com.bumptech.glide.resize.bitmap.FileDescriptorBitmapDecoder;
import com.bumptech.glide.resize.bitmap.StreamBitmapDecoder;
import com.bumptech.glide.resize.load.DecodeFormat;
import com.bumptech.glide.resize.load.Downsampler;
import com.bumptech.glide.resize.load.Transformation;

import java.io.InputStream;

/**
 * A class for creating a request to load a bitmap for an image or from a video. Sets a variety of type independent
 * options including resizing, animations, and placeholders.
 *
 * @param <ModelType> The type of model that will be loaded into the target.
 */
@SuppressWarnings("unused") //public api
public class RequestBuilder<ModelType> extends GenericRequestBuilder<ModelType, InputStream, ParcelFileDescriptor> {

    RequestBuilder(Context context, ModelType model, RequestContext requestContext) {
        super(context, model, requestContext, InputStream.class, ParcelFileDescriptor.class);

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
     *
     * @param downsampler The downsampler
     * @return This RequestBuilder
     */
    private RequestBuilder<ModelType> downsample(Downsampler downsampler) {
        super.imageDecoder(new StreamBitmapDecoder(downsampler, Glide.get(context).getBitmapPool(),
                DecodeFormat.PREFER_RGB_565));
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

    public RequestBuilder<ModelType> imageDecoder(StreamBitmapDecoder decoder) {
        super.imageDecoder(decoder);
        return this;
    }

    public RequestBuilder<ModelType> videoDecoder(FileDescriptorBitmapDecoder decoder) {
        super.videoDecoder(decoder);
        return this;
    }

    @Override
    public RequestBuilder<ModelType> format(DecodeFormat format) {
        super.format(format);
        return this;
    }

    @Override
    public RequestBuilder<ModelType> priority(Priority priority) {
        super.priority(priority);
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
    public RequestBuilder<ModelType> animate(Animation animation) {
        super.animate(animation);
        return this;
    }

    @Override
    public RequestBuilder<ModelType> placeholder(int resourceId) {
        super.placeholder(resourceId);
        return this;
    }

    @Override
    public RequestBuilder<ModelType> placeholder(Drawable drawable) {
        super.placeholder(drawable);
        return this;
    }

    @Override
    public RequestBuilder<ModelType> error(int resourceId) {
        super.error(resourceId);
        return this;
    }

    @Override
    public RequestBuilder<ModelType> error(Drawable drawable) {
        super.error(drawable);
        return this;
    }

    @Override
    public RequestBuilder<ModelType> listener(RequestListener<ModelType> requestListener) {
        super.listener(requestListener);
        return this;
    }
}
