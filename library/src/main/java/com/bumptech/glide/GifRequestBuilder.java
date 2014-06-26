package com.bumptech.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.animation.Animation;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.gif.GifData;
import com.bumptech.glide.load.resource.gif.GifDataTransformation;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.manager.RequestTracker;
import com.bumptech.glide.provider.LoadProvider;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.ViewPropertyAnimation;

import java.io.InputStream;

public class GifRequestBuilder<ModelType, TranscodeType>
        extends GenericRequestBuilder<ModelType, InputStream, GifData, TranscodeType> {
    private Glide glide;

    GifRequestBuilder(Context context, ModelType model,
            LoadProvider<ModelType, InputStream, GifData, TranscodeType> loadProvider,
            Class<TranscodeType> transcodeClass, Glide glide, RequestTracker requestTracker) {
        super(context, model, loadProvider, transcodeClass, glide, requestTracker);
        this.glide = glide;
    }

    @Override
    public GifRequestBuilder<ModelType, TranscodeType> thumbnail(
            GenericRequestBuilder<ModelType, InputStream, GifData, TranscodeType> thumbnailRequest) {
        super.thumbnail(thumbnailRequest);
        return this;
    }

    public GifRequestBuilder<ModelType, TranscodeType> thumbnail(
            GifRequestBuilder<ModelType, TranscodeType> thumbnailRequest) {
        super.thumbnail(thumbnailRequest);
        return this;
    }

    @Override
    public GifRequestBuilder<ModelType, TranscodeType> thumbnail(float sizeMultiplier) {
        super.thumbnail(sizeMultiplier);
        return this;
    }

    @Override
    public GifRequestBuilder<ModelType, TranscodeType> sizeMultiplier(float sizeMultiplier) {
        super.sizeMultiplier(sizeMultiplier);
        return this;
    }

    @Override
    public GifRequestBuilder<ModelType, TranscodeType> decoder(
            ResourceDecoder<InputStream, GifData> decoder) {
        super.decoder(decoder);
        return this;
    }

    @Override
    public GifRequestBuilder<ModelType, TranscodeType> cacheDecoder(
            ResourceDecoder<InputStream, GifData> cacheDecoder) {
        super.cacheDecoder(cacheDecoder);
        return this;
    }

    @Override
    public GifRequestBuilder<ModelType, TranscodeType> encoder(
            ResourceEncoder<GifData> encoder) {
        super.encoder(encoder);
        return this;
    }

    @Override
    public GifRequestBuilder<ModelType, TranscodeType> priority(Priority priority) {
        super.priority(priority);
        return this;
    }

    public GifRequestBuilder<ModelType, TranscodeType> fitCenter() {
        return transformBitmap(new FitCenter(glide.getBitmapPool()));
    }

    public GifRequestBuilder<ModelType, TranscodeType> centerCrop() {
        return transformBitmap(new CenterCrop(glide.getBitmapPool()));
    }

    public GifRequestBuilder<ModelType, TranscodeType> transformBitmap(Transformation<Bitmap> bitmapTransformation) {
        return transform(new GifDataTransformation(bitmapTransformation));
    }

    @Override
    public GifRequestBuilder<ModelType, TranscodeType> transform(Transformation<GifData> transformation) {
        super.transform(transformation);
        return this;
    }

    @Override
    public GifRequestBuilder<ModelType, TranscodeType> transcoder(
            ResourceTranscoder<GifData, TranscodeType> transcoder) {
        super.transcoder(transcoder);
        return this;
    }

    @Override
    public GifRequestBuilder<ModelType, TranscodeType> animate(int animationId) {
        super.animate(animationId);
        return this;
    }

    @Override
    public GifRequestBuilder<ModelType, TranscodeType> animate(Animation animation) {
        super.animate(animation);
        return this;
    }

    @Override
    public GifRequestBuilder<ModelType, TranscodeType> animate(ViewPropertyAnimation.Animator animator) {
        super.animate(animator);
        return this;
    }

    @Override
    public GifRequestBuilder<ModelType, TranscodeType> placeholder(int resourceId) {
        super.placeholder(resourceId);
        return this;
    }

    @Override
    public GifRequestBuilder<ModelType, TranscodeType> placeholder(Drawable drawable) {
        super.placeholder(drawable);
        return this;
    }

    @Override
    public GifRequestBuilder<ModelType, TranscodeType> error(int resourceId) {
        super.error(resourceId);
        return this;
    }

    @Override
    public GifRequestBuilder<ModelType, TranscodeType> error(Drawable drawable) {
        super.error(drawable);
        return this;
    }

    @Override
    public GifRequestBuilder<ModelType, TranscodeType> listener(
            RequestListener<ModelType, TranscodeType> requestListener) {
        super.listener(requestListener);
        return this;
    }

    @Override
    public GifRequestBuilder<ModelType, TranscodeType> skipMemoryCache(boolean skip) {
        super.skipMemoryCache(skip);
        return this;
    }

    @Override
    public GifRequestBuilder<ModelType, TranscodeType> skipDiskCache(boolean skip) {
        super.skipDiskCache( skip);
        return this;
    }

    @Override
    public GifRequestBuilder<ModelType, TranscodeType> skipCache(boolean skip) {
        super.skipCache(skip);
        return this;
    }

    @Override
    public GifRequestBuilder<ModelType, TranscodeType> override(int width, int height) {
        super.override(width, height);
        return this;
    }

    @Override
    public GifRequestBuilder<ModelType, TranscodeType> sourceEncoder(Encoder<InputStream> sourceEncoder) {
        super.sourceEncoder(sourceEncoder);
        return this;
    }

    @Override
    public GifRequestBuilder<ModelType, TranscodeType> cacheSource(boolean cacheSource) {
        super.cacheSource(cacheSource);
        return this;
    }
}
