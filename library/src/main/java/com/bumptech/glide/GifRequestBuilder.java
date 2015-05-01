package com.bumptech.glide;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.animation.Animation;

import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.load.resource.gif.GifDrawableTransformation;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.provider.LoadProvider;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.DrawableCrossFadeFactory;
import com.bumptech.glide.request.animation.ViewPropertyAnimation;

import java.io.File;
import java.io.InputStream;

/**
 * A class for creating a request to load an animated gif.
 *
 * <p>
 *     Warning - It is <em>not</em> safe to use this builder after calling <code>into()</code>, it may be pooled and
 *     reused.
 * </p>
 *
 * @param <ModelType> The type of model that will be loaded into the target.
 */
public class GifRequestBuilder<ModelType>
        extends GenericRequestBuilder<ModelType, InputStream, GifDrawable, GifDrawable>
        implements BitmapOptions, DrawableOptions {

    GifRequestBuilder(LoadProvider<ModelType, InputStream, GifDrawable, GifDrawable> loadProvider,
            Class<GifDrawable> transcodeClass, GenericRequestBuilder<ModelType, ?, ?, ?> other) {
        super(loadProvider, transcodeClass, other);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType> thumbnail(GenericRequestBuilder<?, ?, ?, GifDrawable> thumbnailRequest) {
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
    public GifRequestBuilder<ModelType> thumbnail(GifRequestBuilder<?> thumbnailRequest) {
        super.thumbnail(thumbnailRequest);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType> thumbnail(float sizeMultiplier) {
        super.thumbnail(sizeMultiplier);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType> sizeMultiplier(float sizeMultiplier) {
        super.sizeMultiplier(sizeMultiplier);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType> decoder(
            ResourceDecoder<InputStream, GifDrawable> decoder) {
        super.decoder(decoder);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType> cacheDecoder(
            ResourceDecoder<File, GifDrawable> cacheDecoder) {
        super.cacheDecoder(cacheDecoder);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType> encoder(
            ResourceEncoder<GifDrawable> encoder) {
        super.encoder(encoder);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType> priority(Priority priority) {
        super.priority(priority);
        return this;
    }

    /**
     * Transforms each frame of the GIF using {@link com.bumptech.glide.load.resource.bitmap.CenterCrop}.
     *
     * @see #fitCenter()
     * @see #transformFrame(com.bumptech.glide.load.resource.bitmap.BitmapTransformation...)
     * @see #transformFrame(com.bumptech.glide.load.Transformation[])
     * @see #transform(com.bumptech.glide.load.Transformation[])
     *
     * @return This request builder.
     */
    public GifRequestBuilder<ModelType> centerCrop() {
        return transformFrame(glide.getBitmapCenterCrop());
    }

    /**
     * Transforms each frame of the GIF using {@link com.bumptech.glide.load.resource.bitmap.FitCenter}.
     *
     * @see #centerCrop()
     * @see #transformFrame(com.bumptech.glide.load.resource.bitmap.BitmapTransformation...)
     * @see #transformFrame(com.bumptech.glide.load.Transformation[])
     * @see #transform(com.bumptech.glide.load.Transformation[])
     *
     * @return This request builder..
     */
    public GifRequestBuilder<ModelType> fitCenter() {
        return transformFrame(glide.getBitmapFitCenter());
    }

    /**
     * Transforms each frame of the GIF using the given transformations.
     *
     * @see #centerCrop()
     * @see #fitCenter()
     * @see #transformFrame(com.bumptech.glide.load.Transformation[])
     * @see #transform(com.bumptech.glide.load.Transformation[])
     *
     * @param bitmapTransformations The transformations to apply in order to each frame.
     * @return This request builder.
     */
    public GifRequestBuilder<ModelType> transformFrame(BitmapTransformation... bitmapTransformations) {
        return transform(toGifTransformations(bitmapTransformations));
    }

    /**
     * Transforms each frame of the GIF using the given transformations.
     *
     * @see #fitCenter()
     * @see #centerCrop()
     * @see #transformFrame(com.bumptech.glide.load.resource.bitmap.BitmapTransformation...)
     * @see #transform(com.bumptech.glide.load.Transformation[])
     *
     * @param bitmapTransformations The transformations to apply in order to each frame.
     * @return This request builder.
     */
    public GifRequestBuilder<ModelType> transformFrame(Transformation<Bitmap>... bitmapTransformations) {
        return transform(toGifTransformations(bitmapTransformations));
    }

    private GifDrawableTransformation[] toGifTransformations(Transformation<Bitmap>[] bitmapTransformations) {
        GifDrawableTransformation[] transformations = new GifDrawableTransformation[bitmapTransformations.length];
        for (int i = 0; i < bitmapTransformations.length; i++) {
            transformations[i] = new GifDrawableTransformation(bitmapTransformations[i], glide.getBitmapPool());
        }
        return transformations;
    }

    /**
     * {@inheritDoc}
     *
     * @see #fitCenter()
     * @see #centerCrop()
     * @see #transformFrame(com.bumptech.glide.load.resource.bitmap.BitmapTransformation...)
     * @see #transformFrame(com.bumptech.glide.load.Transformation[])
     *
     */
    @Override
    public GifRequestBuilder<ModelType> transform(Transformation<GifDrawable>... transformations) {
        super.transform(transformations);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType> transcoder(ResourceTranscoder<GifDrawable, GifDrawable> transcoder) {
        super.transcoder(transcoder);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType> crossFade() {
        super.animate(new DrawableCrossFadeFactory<GifDrawable>());
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType> crossFade(int duration) {
        super.animate(new DrawableCrossFadeFactory<GifDrawable>(duration));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    @Override
    public GifRequestBuilder<ModelType> crossFade(Animation animation, int duration) {
        super.animate(new DrawableCrossFadeFactory<GifDrawable>(animation, duration));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType> crossFade(int animationId, int duration) {
        super.animate(new DrawableCrossFadeFactory<GifDrawable>(context, animationId,
                duration));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType> dontAnimate() {
        super.dontAnimate();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType> animate(int animationId) {
        super.animate(animationId);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    @Override
    public GifRequestBuilder<ModelType> animate(Animation animation) {
        super.animate(animation);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType> animate(ViewPropertyAnimation.Animator animator) {
        super.animate(animator);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType> placeholder(int resourceId) {
        super.placeholder(resourceId);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType> placeholder(Drawable drawable) {
        super.placeholder(drawable);
        return this;
    }

    @Override
    public GifRequestBuilder<ModelType> fallback(Drawable drawable) {
        super.fallback(drawable);
        return this;
    }

    @Override
    public GifRequestBuilder<ModelType> fallback(int resourceId) {
        super.fallback(resourceId);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType> error(int resourceId) {
        super.error(resourceId);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType> error(Drawable drawable) {
        super.error(drawable);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType> listener(
            RequestListener<? super ModelType, GifDrawable> requestListener) {
        super.listener(requestListener);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType> skipMemoryCache(boolean skip) {
        super.skipMemoryCache(skip);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType> diskCacheStrategy(DiskCacheStrategy strategy) {
        super.diskCacheStrategy(strategy);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType> override(int width, int height) {
        super.override(width, height);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType> sourceEncoder(Encoder<InputStream> sourceEncoder) {
        super.sourceEncoder(sourceEncoder);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType> dontTransform() {
        super.dontTransform();
        return this;
    }

    @Override
    public GifRequestBuilder<ModelType> signature(Key signature) {
        super.signature(signature);
        return this;
    }

    @Override
    public GifRequestBuilder<ModelType> load(ModelType model) {
        super.load(model);
        return this;
    }

    @Override
    public GifRequestBuilder<ModelType> clone() {
        return (GifRequestBuilder<ModelType>) super.clone();
    }

    @Override
    void applyFitCenter() {
        fitCenter();
    }

    @Override
    void applyCenterCrop() {
        centerCrop();
    }
}
