package com.bumptech.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.animation.Animation;
import android.widget.ImageView;

import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.load.resource.gif.GifData;
import com.bumptech.glide.load.resource.gif.GifDataTransformation;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.manager.RequestTracker;
import com.bumptech.glide.provider.LoadProvider;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.DrawableCrossFadeViewAnimation;
import com.bumptech.glide.request.animation.ViewPropertyAnimation;
import com.bumptech.glide.request.target.Target;

import java.io.File;
import java.io.InputStream;

/**
 * A class for creating a request to load an animated gif.
 *
 * @param <ModelType> The type of model that will be loaded into the target.
 */
public class GifRequestBuilder<ModelType> extends GenericRequestBuilder<ModelType, InputStream, GifData, GifDrawable>
        implements BitmapOptions, DrawableOptions {
    private final Context context;
    private final Glide glide;

    GifRequestBuilder(Context context, ModelType model,
            LoadProvider<ModelType, InputStream, GifData, GifDrawable> loadProvider,
            Glide glide, RequestTracker requestTracker) {
        super(context, model, loadProvider, GifDrawable.class, glide, requestTracker);
        this.context = context;
        this.glide = glide;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType> thumbnail(
            GenericRequestBuilder<ModelType, InputStream, GifData, GifDrawable> thumbnailRequest) {
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
    public GifRequestBuilder<ModelType> thumbnail(GifRequestBuilder<ModelType> thumbnailRequest) {
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
            ResourceDecoder<InputStream, GifData> decoder) {
        super.decoder(decoder);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType> cacheDecoder(
            ResourceDecoder<File, GifData> cacheDecoder) {
        super.cacheDecoder(cacheDecoder);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType> encoder(
            ResourceEncoder<GifData> encoder) {
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

    private static GifDataTransformation[] toGifTransformations(Transformation<Bitmap>[] bitmapTransformations) {
        GifDataTransformation[] transformations = new GifDataTransformation[bitmapTransformations.length];
        for (int i = 0; i < bitmapTransformations.length; i++) {
            transformations[i] = new GifDataTransformation(bitmapTransformations[i]);
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
    public GifRequestBuilder<ModelType> transform(Transformation<GifData>... transformations) {
        super.transform(transformations);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType> transcoder(ResourceTranscoder<GifData, GifDrawable> transcoder) {
        super.transcoder(transcoder);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType> crossFade() {
        super.animate(new DrawableCrossFadeViewAnimation.DrawableCrossFadeFactory<GifDrawable>());
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType> crossFade(int duration) {
        super.animate(new DrawableCrossFadeViewAnimation.DrawableCrossFadeFactory<GifDrawable>(duration));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType> crossFade(Animation animation, int duration) {
        super.animate(new DrawableCrossFadeViewAnimation.DrawableCrossFadeFactory<GifDrawable>(animation, duration));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GifRequestBuilder<ModelType> crossFade(int animationId, int duration) {
        super.animate(new DrawableCrossFadeViewAnimation.DrawableCrossFadeFactory<GifDrawable>(context, animationId,
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
            RequestListener<ModelType, GifDrawable> requestListener) {
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

    /**
     * {@inheritDoc}
     *
     * <p>
     *     Note - If no transformation is set for this load, a default transformation will be applied based on the
     *     value returned from {@link android.widget.ImageView#getScaleType()}. To avoid this default transformation,
     *     use {@link #dontTransform()}.
     * </p>
     *
     * @param view {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Target<GifDrawable> into(ImageView view) {
        return super.into(view);
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
