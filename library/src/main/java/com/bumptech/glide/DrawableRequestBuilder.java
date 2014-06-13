package com.bumptech.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.animation.Animation;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.model.ImageVideoWrapper;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.gifbitmap.GifBitmapWrapper;
import com.bumptech.glide.load.resource.gifbitmap.GifBitmapWrapperTransformation;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.provider.LoadProvider;
import com.bumptech.glide.request.bitmap.RequestListener;

import java.io.InputStream;

public class DrawableRequestBuilder<ModelType> extends
        GenericRequestBuilder<ModelType, ImageVideoWrapper, GifBitmapWrapper, Drawable> {
    private final Context context;
    private final Glide glide;

    DrawableRequestBuilder(Context context, ModelType model,
            LoadProvider<ModelType, ImageVideoWrapper, GifBitmapWrapper, Drawable> loadProvider, Glide glide) {
        super(context, model, loadProvider, Drawable.class, glide);
        this.context = context;
        this.glide = glide;
    }

    public DrawableRequestBuilder<ModelType> thumbnail(
            DrawableRequestBuilder<ModelType> thumbnailRequest) {
        super.thumbnail(thumbnailRequest);
        return this;
    }

    @Override
    public DrawableRequestBuilder<ModelType> thumbnail(
            GenericRequestBuilder<ModelType, ImageVideoWrapper, GifBitmapWrapper, Drawable> thumbnailRequest) {
        super.thumbnail(thumbnailRequest);
        return this;
    }

    @Override
    public DrawableRequestBuilder<ModelType> thumbnail(float sizeMultiplier) {
        super.thumbnail(sizeMultiplier);
        return this;
    }

    @Override
    public DrawableRequestBuilder<ModelType> sizeMultiplier(
            float sizeMultiplier) {
        super.sizeMultiplier(sizeMultiplier);
        return this;
    }

    @Override
    public DrawableRequestBuilder<ModelType> decoder(
            ResourceDecoder<ImageVideoWrapper, GifBitmapWrapper> decoder) {
        super.decoder(decoder);
        return this;
    }

    @Override
    public DrawableRequestBuilder<ModelType> cacheDecoder(
            ResourceDecoder<InputStream, GifBitmapWrapper> cacheDecoder) {
        super.cacheDecoder(cacheDecoder);
        return this;
    }

    @Override
    public DrawableRequestBuilder<ModelType> encoder(
            ResourceEncoder<GifBitmapWrapper> encoder) {
        super.encoder(encoder);
        return this;
    }

    @Override
    public DrawableRequestBuilder<ModelType> priority(Priority priority) {
        super.priority(priority);
        return this;
    }

    public DrawableRequestBuilder<ModelType> centerCrop() {
        return bitmapTransform(new CenterCrop(glide.getBitmapPool()));
    }

    public DrawableRequestBuilder<ModelType> fitCenter() {
        return bitmapTransform(new FitCenter(glide.getBitmapPool()));
    }

    public DrawableRequestBuilder<ModelType> bitmapTransform(Transformation<Bitmap> bitmapTransformation) {
        return transform(new GifBitmapWrapperTransformation(context, bitmapTransformation));
    }

    @Override
    public DrawableRequestBuilder<ModelType> transform(
            Transformation<GifBitmapWrapper> transformation) {
        super.transform(transformation);
        return this;
    }

    @Override
    public DrawableRequestBuilder<ModelType> transcoder(
            ResourceTranscoder<GifBitmapWrapper, Drawable> transcoder) {
        super.transcoder(transcoder);
        return this;
    }

    @Override
    public DrawableRequestBuilder<ModelType> animate(int animationId) {
        super.animate(animationId);
        return this;
    }

    @Override
    public DrawableRequestBuilder<ModelType> animate(Animation animation) {
        super.animate(animation);
        return this;
    }

    @Override
    public DrawableRequestBuilder<ModelType> placeholder(int resourceId) {
        super.placeholder(resourceId);
        return this;
    }

    @Override
    public DrawableRequestBuilder<ModelType> placeholder(Drawable drawable) {
        super.placeholder(drawable);
        return this;
    }

    @Override
    public DrawableRequestBuilder<ModelType> error(int resourceId) {
        super.error(resourceId);
        return this;
    }

    @Override
    public DrawableRequestBuilder<ModelType> error(Drawable drawable) {
        super.error(drawable);
        return this;
    }

    @Override
    public DrawableRequestBuilder<ModelType> listener(
            RequestListener<ModelType> requestListener) {
        super.listener(requestListener);
        return this;
    }

    @Override
    public DrawableRequestBuilder<ModelType> skipMemoryCache(boolean skip) {
        super.skipMemoryCache(skip);
        return this;
    }

    @Override
    public DrawableRequestBuilder<ModelType> skipDiskCache(boolean skip) {
        super.skipDiskCache(skip);
        return this;
    }

    @Override
    public DrawableRequestBuilder<ModelType> skipCache(boolean skip) {
        super.skipCache(skip);
        return this;
    }
}
