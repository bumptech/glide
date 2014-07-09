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
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.gif.GifData;
import com.bumptech.glide.load.resource.gif.GifDataTransformation;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.manager.RequestTracker;
import com.bumptech.glide.provider.LoadProvider;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.ViewPropertyAnimation;

import java.io.File;
import java.io.InputStream;

/**
 * A class for creating a request to load an animated gif.
 *
 * @param <ModelType> The type of model that will be loaded into the target.
 * @param <TranscodeType> The type of the resource class the GifData will be transcoded to.
 */
public class GifRequestBuilder<ModelType, TranscodeType>
        extends GenericRequestBuilder<ModelType, InputStream, GifData, TranscodeType>
        implements BitmapOptions {
    private Glide glide;

    GifRequestBuilder(Context context, ModelType model,
            LoadProvider<ModelType, InputStream, GifData, TranscodeType> loadProvider,
            Class<TranscodeType> transcodeClass, Glide glide, RequestTracker requestTracker) {
        super(context, model, loadProvider, transcodeClass, glide, requestTracker);
        this.glide = glide;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType, TranscodeType> thumbnail(
            GenericRequestBuilder<ModelType, InputStream, GifData, TranscodeType> thumbnailRequest) {
        super.thumbnail(thumbnailRequest);
        return this;
    }

    /**
     * Loads and displays the GIF retrieved by the given thumbnail request if it finishes before this
     * request. Best used for loading thumbnail GIFs that are smaller and will be loaded more quickly
     * than the fullsize GIF. There are no guarantees about the order in which the requests will actually
     * finish. However, if the thumb request completes after the full request, the thumb GIF will never
     * replace the full image.
     *
     * @see #thumbnail(float)
     *
     * <p>
     *     Note - Any options on the main request will not be passed on to the thumbnail request. For example, if
     *     you want an animation to occur when either the full GIF loads or the thumbnail loads,
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
    public GifRequestBuilder<ModelType, TranscodeType> thumbnail(
            GifRequestBuilder<ModelType, TranscodeType> thumbnailRequest) {
        super.thumbnail(thumbnailRequest);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType, TranscodeType> thumbnail(float sizeMultiplier) {
        super.thumbnail(sizeMultiplier);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType, TranscodeType> sizeMultiplier(float sizeMultiplier) {
        super.sizeMultiplier(sizeMultiplier);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType, TranscodeType> decoder(
            ResourceDecoder<InputStream, GifData> decoder) {
        super.decoder(decoder);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType, TranscodeType> cacheDecoder(
            ResourceDecoder<File, GifData> cacheDecoder) {
        super.cacheDecoder(cacheDecoder);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType, TranscodeType> encoder(
            ResourceEncoder<GifData> encoder) {
        super.encoder(encoder);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType, TranscodeType> priority(Priority priority) {
        super.priority(priority);
        return this;
    }

    /**
     * Transforms each frame of the GIF using {@link com.bumptech.glide.load.resource.bitmap.CenterCrop}.
     *
     * @see #transformFrame(com.bumptech.glide.load.Transformation)
     *
     * @return This request builder.
     */
    public GifRequestBuilder<ModelType, TranscodeType> centerCrop() {
        return transformFrame(new CenterCrop(glide.getBitmapPool()));
    }

    /**
     * Transforms each frame of the GIF using {@link com.bumptech.glide.load.resource.bitmap.FitCenter}.
     *
     * @see #transformFrame(com.bumptech.glide.load.Transformation)
     *
     * @return This request builder..
     */
    public GifRequestBuilder<ModelType, TranscodeType> fitCenter() {
        return transformFrame(new FitCenter(glide.getBitmapPool()));
    }

    /**
     * Transforms each frame of the GIF using the given transformation.
     *
     * @param bitmapTransformation The transformation to use.
     * @return This request builder.
     */
    public GifRequestBuilder<ModelType, TranscodeType> transformFrame(Transformation<Bitmap> bitmapTransformation) {
        return transform(new GifDataTransformation(bitmapTransformation));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType, TranscodeType> transform(Transformation<GifData> transformation) {
        super.transform(transformation);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType, TranscodeType> transcoder(
            ResourceTranscoder<GifData, TranscodeType> transcoder) {
        super.transcoder(transcoder);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType, TranscodeType> dontAnimate() {
        super.dontAnimate();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType, TranscodeType> animate(int animationId) {
        super.animate(animationId);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType, TranscodeType> animate(Animation animation) {
        super.animate(animation);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType, TranscodeType> animate(ViewPropertyAnimation.Animator animator) {
        super.animate(animator);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType, TranscodeType> placeholder(int resourceId) {
        super.placeholder(resourceId);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType, TranscodeType> placeholder(Drawable drawable) {
        super.placeholder(drawable);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType, TranscodeType> error(int resourceId) {
        super.error(resourceId);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType, TranscodeType> error(Drawable drawable) {
        super.error(drawable);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType, TranscodeType> listener(
            RequestListener<ModelType, TranscodeType> requestListener) {
        super.listener(requestListener);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType, TranscodeType> skipMemoryCache(boolean skip) {
        super.skipMemoryCache(skip);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType, TranscodeType> diskCacheStrategy(DiskCacheStrategy strategy) {
        super.diskCacheStrategy(strategy);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType, TranscodeType> override(int width, int height) {
        super.override(width, height);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType, TranscodeType> sourceEncoder(Encoder<InputStream> sourceEncoder) {
        super.sourceEncoder(sourceEncoder);
        return this;
    }
}
