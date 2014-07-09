package com.bumptech.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.animation.Animation;

import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.ImageVideoWrapper;
import com.bumptech.glide.load.resource.gifbitmap.GifBitmapWrapper;
import com.bumptech.glide.load.resource.gifbitmap.GifBitmapWrapperTransformation;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.manager.RequestTracker;
import com.bumptech.glide.provider.LoadProvider;
import com.bumptech.glide.request.animation.DrawableCrossFadeViewAnimation;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.ViewPropertyAnimation;

import java.io.File;

/**
 * A class for creating a request to load a {@link android.graphics.drawable.Drawable}.
 *
 * @param <ModelType> The type of model that will be loaded into the target.
 */
public class DrawableRequestBuilder<ModelType>
        extends GenericRequestBuilder<ModelType, ImageVideoWrapper, GifBitmapWrapper, Drawable>
        implements BitmapOptions, DrawableOptions {
    private final Glide glide;
    private final Context context;

    DrawableRequestBuilder(Context context, ModelType model,
            LoadProvider<ModelType, ImageVideoWrapper, GifBitmapWrapper, Drawable> loadProvider, Glide glide,
            RequestTracker requestTracker) {
        super(context, model, loadProvider, Drawable.class, glide, requestTracker);
        this.context = context;
        this.glide = glide;
    }

    /**
     * Loads and displays the {@link Drawable} retrieved by the given thumbnail request if it finishes before this
     * request. Best used for loading thumbnail {@link Drawable}s that are smaller and will be loaded more quickly
     * than the fullsize {@link Drawable}. There are no guarantees about the order in which the requests will actually
     * finish. However, if the thumb request completes after the full request, the thumb {@link Drawable} will never
     * replace the full image.
     *
     * @see #thumbnail(float)
     *
     * <p>
     *     Note - Any options on the main request will not be passed on to the thumbnail request. For example, if
     *     you want an animation to occur when either the full {@link Drawable} loads or the thumbnail loads,
     *     you need to call {@link #animate(int)} on both the thumb and the full request. For a simpler thumbnail
     *     option where these options are applied to the humbnail as well, see {@link #thumbnail(float)}.
     * </p>
     *
     * <p>
     *     Only the thumbnail call on the main request will be obeyed, recursive calls to this method are ignored.
     * </p>
     *
     * @param thumbnailRequest The request to use to load the thumbnail.
     * @return This builder object.
     */
    public DrawableRequestBuilder<ModelType> thumbnail(
            DrawableRequestBuilder<ModelType> thumbnailRequest) {
        super.thumbnail(thumbnailRequest);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DrawableRequestBuilder<ModelType> thumbnail(
            GenericRequestBuilder<ModelType, ImageVideoWrapper, GifBitmapWrapper, Drawable> thumbnailRequest) {
        super.thumbnail(thumbnailRequest);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DrawableRequestBuilder<ModelType> thumbnail(float sizeMultiplier) {
        super.thumbnail(sizeMultiplier);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DrawableRequestBuilder<ModelType> sizeMultiplier(
            float sizeMultiplier) {
        super.sizeMultiplier(sizeMultiplier);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DrawableRequestBuilder<ModelType> decoder(
            ResourceDecoder<ImageVideoWrapper, GifBitmapWrapper> decoder) {
        super.decoder(decoder);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DrawableRequestBuilder<ModelType> cacheDecoder(
            ResourceDecoder<File, GifBitmapWrapper> cacheDecoder) {
        super.cacheDecoder(cacheDecoder);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DrawableRequestBuilder<ModelType> encoder(
            ResourceEncoder<GifBitmapWrapper> encoder) {
        super.encoder(encoder);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DrawableRequestBuilder<ModelType> priority(Priority priority) {
        super.priority(priority);
        return this;
    }

    /**
     * Transform {@link Drawable}s using {@link com.bumptech.glide.load.resource.bitmap.CenterCrop}.
     *
     * @return This request builder.
     */
    public DrawableRequestBuilder<ModelType> centerCrop() {
        return transform(glide.getDrawableCenterCrop());
    }

    /**
     * Transform {@link android.graphics.drawable.Drawable}s using
     * {@link com.bumptech.glide.load.resource.bitmap.FitCenter}.
     *
     * @return This request builder.
     */
    public DrawableRequestBuilder<ModelType> fitCenter() {
        return transform(glide.getDrawableFitCenter());
    }

    /**
     * Transform {@link android.graphics.drawable.Drawable}s using the given bitmap transformation.
     *
     * @return This request builder.
     */
    public DrawableRequestBuilder<ModelType> bitmapTransform(Transformation<Bitmap> bitmapTransformation) {
        return transform(new GifBitmapWrapperTransformation(bitmapTransformation));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DrawableRequestBuilder<ModelType> transform(Transformation<GifBitmapWrapper> transformation) {
        super.transform(transformation);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DrawableRequestBuilder<ModelType> transcoder(
            ResourceTranscoder<GifBitmapWrapper, Drawable> transcoder) {
        super.transcoder(transcoder);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public DrawableRequestBuilder<ModelType> crossFade() {
        super.animate(new DrawableCrossFadeViewAnimation.DrawableCrossFadeFactory<Drawable>());
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public DrawableRequestBuilder<ModelType> crossFade(int duration) {
        super.animate(new DrawableCrossFadeViewAnimation.DrawableCrossFadeFactory<Drawable>(duration));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public DrawableRequestBuilder<ModelType> crossFade(Animation animation, int duration) {
        super.animate(new DrawableCrossFadeViewAnimation.DrawableCrossFadeFactory<Drawable>(animation, duration));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public DrawableRequestBuilder<ModelType> crossFade(int animationId, int duration) {
        super.animate(new DrawableCrossFadeViewAnimation.DrawableCrossFadeFactory<Drawable>(context, animationId,
                duration));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DrawableRequestBuilder<ModelType> animate(ViewPropertyAnimation.Animator animator) {
        super.animate(animator);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DrawableRequestBuilder<ModelType> animate(int animationId) {
        super.animate(animationId);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DrawableRequestBuilder<ModelType> animate(Animation animation) {
        super.animate(animation);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DrawableRequestBuilder<ModelType> placeholder(int resourceId) {
        super.placeholder(resourceId);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DrawableRequestBuilder<ModelType> placeholder(Drawable drawable) {
        super.placeholder(drawable);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DrawableRequestBuilder<ModelType> error(int resourceId) {
        super.error(resourceId);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DrawableRequestBuilder<ModelType> error(Drawable drawable) {
        super.error(drawable);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DrawableRequestBuilder<ModelType> listener(RequestListener<ModelType, Drawable> requestListener) {
        super.listener(requestListener);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DrawableRequestBuilder<ModelType> diskCacheStrategy(DiskCacheStrategy strategy) {
        super.diskCacheStrategy(strategy);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DrawableRequestBuilder<ModelType> skipMemoryCache(boolean skip) {
        super.skipMemoryCache(skip);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DrawableRequestBuilder<ModelType> override(int width, int height) {
        super.override(width, height);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DrawableRequestBuilder<ModelType> sourceEncoder(Encoder<ImageVideoWrapper> sourceEncoder) {
        super.sourceEncoder(sourceEncoder);
        return this;
    }
}
