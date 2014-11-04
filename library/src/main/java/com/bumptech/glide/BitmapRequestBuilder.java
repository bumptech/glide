package com.bumptech.glide;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.ParcelFileDescriptor;
import android.view.animation.Animation;
import android.widget.ImageView;

import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.model.ImageVideoWrapper;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.load.resource.bitmap.Downsampler;
import com.bumptech.glide.load.resource.bitmap.FileDescriptorBitmapDecoder;
import com.bumptech.glide.load.resource.bitmap.ImageVideoBitmapDecoder;
import com.bumptech.glide.load.resource.bitmap.StreamBitmapDecoder;
import com.bumptech.glide.load.resource.bitmap.VideoBitmapDecoder;
import com.bumptech.glide.load.resource.file.FileToStreamDecoder;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.provider.LoadProvider;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.ViewPropertyAnimation;
import com.bumptech.glide.request.target.Target;

import java.io.File;
import java.io.InputStream;

/**
 * A class for creating a request to load a bitmap for an image or from a video. Sets a variety of type independent
 * options including resizing, animations, and placeholders.
 *
 * <p>
 *     Warning - It is <em>not</em> safe to use this builder after calling <code>into()</code>, it may be pooled and
 *     reused.
 * </p>
 *
 * @param <ModelType> The type of model that will be loaded into the target.
 * @param <TranscodeType> The type of the transcoded resource that the target will receive
 */
public class BitmapRequestBuilder<ModelType, TranscodeType>
        extends GenericRequestBuilder<ModelType, ImageVideoWrapper, Bitmap, TranscodeType> implements BitmapOptions {
    private final BitmapPool bitmapPool;

    private Downsampler downsampler = Downsampler.AT_LEAST;
    private DecodeFormat decodeFormat;
    private ResourceDecoder<InputStream, Bitmap> imageDecoder;
    private ResourceDecoder<ParcelFileDescriptor, Bitmap> videoDecoder;

    BitmapRequestBuilder(LoadProvider<ModelType, ImageVideoWrapper, Bitmap, TranscodeType> loadProvider,
            Class<TranscodeType> transcodeClass, GenericRequestBuilder<ModelType, ?, ?, ?> other) {
        super(loadProvider, transcodeClass, other);
        this.bitmapPool = other.glide.getBitmapPool();
        this.decodeFormat =  other.glide.getDecodeFormat();

        imageDecoder = new StreamBitmapDecoder(bitmapPool, decodeFormat);
        videoDecoder = new FileDescriptorBitmapDecoder(bitmapPool, decodeFormat);
    }

    /**
     * Load images at a size near the size of the target using {@link Downsampler#AT_LEAST}.
     *
     * @see #downsample(Downsampler)
     *
     * @return This request builder.
     */
    public BitmapRequestBuilder<ModelType, TranscodeType> approximate() {
        return downsample(Downsampler.AT_LEAST);
    }

    /**
     * Load images at their original size using {@link Downsampler#NONE}.
     *
     * @see #downsample(Downsampler)
     *
     * @return This request builder.
     */
    public BitmapRequestBuilder<ModelType, TranscodeType> asIs() {
        return downsample(Downsampler.NONE);
    }

    /**
     * Load images at a size that is at most exactly as big as the target using
     * {@link com.bumptech.glide.load.resource.bitmap.Downsampler#AT_MOST}.
     *
     * @see #downsample(com.bumptech.glide.load.resource.bitmap.Downsampler)
     *
     * @return This request builder.
     */
    public BitmapRequestBuilder<ModelType, TranscodeType> atMost() {
        return downsample(Downsampler.AT_MOST);
    }

    /**
     * Load images using the given {@link Downsampler}. Replaces any existing image decoder. Defaults to
     * {@link Downsampler#AT_LEAST}. Will be ignored if the data represented by the model is a video. This replaces any
     * previous calls to {@link #imageDecoder(ResourceDecoder)}  and {@link #decoder(ResourceDecoder)} with default
     * decoders with the appropriate options set.
     *
     * @see #imageDecoder
     *
     * @param downsampler The downsampler.
     * @return This request builder.
     */
    private BitmapRequestBuilder<ModelType, TranscodeType> downsample(Downsampler downsampler) {
        this.downsampler = downsampler;
        imageDecoder = new StreamBitmapDecoder(downsampler, bitmapPool, decodeFormat);
        super.decoder(new ImageVideoBitmapDecoder(imageDecoder, videoDecoder));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> thumbnail(float sizeMultiplier) {
        super.thumbnail(sizeMultiplier);
        return this;
    }

    /**
     * Loads and displays the {@link android.graphics.Bitmap} retrieved by the given thumbnail request if it finishes
     * before this request. Best used for loading thumbnail {@link Bitmap}s that are smaller and will be loaded more
     * quickly than the fullsize {@link Bitmap}. There are no guarantees about the order in which the requests will
     * actually finish. However, if the thumb request completes after the full request, the thumb
     * {@link android.graphics.Bitmap} will never replace the full image.
     *
     * @see #thumbnail(float)
     *
     * <p>
     *     Note - Any options on the main request will not be passed on to the thumbnail request. For example, if
     *     you want an animation to occur when either the full {@link android.graphics.Bitmap} loads or the thumbnail
     *     loads, you need to call {@link #animate(int)} on both the thumb and the full request. For a simpler thumbnail
     *     option where these options are applied to the humbnail as well, see {@link #thumbnail(float)}.
     * </p>
     *
     * <p>
     *     Only the thumbnail call on the main request will be obeyed, recursive calls to this method are ignored.
     * </p>
     *
     * @param thumbnailRequest The request to use to load the thumbnail.
     * @return This request builder.
     */
    public BitmapRequestBuilder<ModelType, TranscodeType> thumbnail(BitmapRequestBuilder<?, TranscodeType>
            thumbnailRequest) {
        super.thumbnail(thumbnailRequest);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> sizeMultiplier(float sizeMultiplier) {
        super.sizeMultiplier(sizeMultiplier);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> decoder(ResourceDecoder<ImageVideoWrapper, Bitmap> decoder) {
        super.decoder(decoder);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> cacheDecoder(ResourceDecoder<File, Bitmap> cacheDecoder) {
        super.cacheDecoder(cacheDecoder);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> encoder(ResourceEncoder<Bitmap> encoder) {
        super.encoder(encoder);
        return this;
    }

    /**
     * Sets the {@link com.bumptech.glide.load.ResourceDecoder} that will be used to decode {@link Bitmap}s obtained
     * from an {@link java.io.InputStream}.
     *
     * @see #videoDecoder
     *
     * @param decoder The decoder to use to decode {@link Bitmap}s.
     * @return This request builder.
     */
    public BitmapRequestBuilder<ModelType, TranscodeType> imageDecoder(ResourceDecoder<InputStream, Bitmap> decoder) {
        imageDecoder = decoder;
        super.decoder(new ImageVideoBitmapDecoder(decoder, videoDecoder));
        return this;
    }

    /**
     * Sets the {@link com.bumptech.glide.load.ResourceDecoder} that will be used to decode {@link Bitmap}s obtained
     * from an {@link android.os.ParcelFileDescriptor}.
     *
     * @param decoder The decoder to use to decode {@link Bitmap}s.
     * @return This request builder.
     */
    public BitmapRequestBuilder<ModelType, TranscodeType> videoDecoder(
            ResourceDecoder<ParcelFileDescriptor, Bitmap> decoder) {
        videoDecoder = decoder;
        super.decoder(new ImageVideoBitmapDecoder(imageDecoder, decoder));
        return this;
    }

    /**
     * Sets the preferred format for {@link Bitmap}s decoded in this request. Defaults to
     * {@link DecodeFormat#PREFER_RGB_565}. This replaces any previous calls to {@link #imageDecoder(ResourceDecoder)},
     * {@link #videoDecoder(ResourceDecoder)}, {@link #decoder(ResourceDecoder)} and
     * {@link #cacheDecoder(com.bumptech.glide.load.ResourceDecoder)}} with default decoders with the appropriate
     * options set.
     *
     * <p>
     *     Note - If using a {@link Transformation} that expect bitmaps to support transparency, this should always be
     *     set to ALWAYS_ARGB_8888. RGB_565 requires fewer bytes per pixel and is generally preferable, but it does not
     *     support transparency.
     * </p>
     *
     * @see DecodeFormat
     *
     * @param format The format to use.
     * @return This request builder.
     */
    public BitmapRequestBuilder<ModelType, TranscodeType> format(DecodeFormat format) {
        this.decodeFormat = format;
        imageDecoder = new StreamBitmapDecoder(downsampler, bitmapPool, format);
        videoDecoder = new FileDescriptorBitmapDecoder(new VideoBitmapDecoder(), bitmapPool, format);
        super.cacheDecoder(new FileToStreamDecoder<Bitmap>(new StreamBitmapDecoder(downsampler, bitmapPool, format)));
        super.decoder(new ImageVideoBitmapDecoder(imageDecoder, videoDecoder));
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> priority(Priority priority) {
        super.priority(priority);
        return this;
    }

    /**
     * Transform images using the given {@link com.bumptech.glide.load.resource.bitmap.BitmapTransformation}s.
     *
     * @see #centerCrop()
     * @see #fitCenter()
     * @see #transform(com.bumptech.glide.load.Transformation[])
     *
     * @param transformations The transformations to apply in order.
     * @return This request builder.
     */
    public BitmapRequestBuilder<ModelType, TranscodeType> transform(BitmapTransformation... transformations) {
        super.transform(transformations);
        return this;
    }

    /**
     * Transform images using {@link com.bumptech.glide.load.resource.bitmap.CenterCrop}.
     *
     * @see #fitCenter()
     * @see #transform(com.bumptech.glide.load.resource.bitmap.BitmapTransformation...)
     * @see #transform(com.bumptech.glide.load.Transformation[])
     *
     * @return This request builder.
     */
    public BitmapRequestBuilder<ModelType, TranscodeType> centerCrop() {
        return transform(glide.getBitmapCenterCrop());
    }

    /**
     * Transform images using {@link com.bumptech.glide.load.resource.bitmap.FitCenter}.
     *
     * @see #centerCrop()
     * @see #transform(com.bumptech.glide.load.resource.bitmap.BitmapTransformation...)
     * @see #transform(com.bumptech.glide.load.Transformation[])
     *
     * @return This request builder.
     */
    public BitmapRequestBuilder<ModelType, TranscodeType> fitCenter() {
        return transform(glide.getBitmapFitCenter());
    }

    /**
     * {@inheritDoc}
     *
     * @see #fitCenter()
     * @see #centerCrop()
     */
    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> transform(Transformation<Bitmap>... transformations) {
        super.transform(transformations);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> transcoder(
            ResourceTranscoder<Bitmap, TranscodeType> transcoder) {
        super.transcoder(transcoder);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> dontAnimate() {
        super.dontAnimate();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> animate(int animationId) {
        super.animate(animationId);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> animate(Animation animation) {
        super.animate(animation);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> animate(ViewPropertyAnimation.Animator animator) {
        super.animate(animator);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> placeholder(int resourceId) {
        super.placeholder(resourceId);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> placeholder(Drawable drawable) {
        super.placeholder(drawable);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> error(int resourceId) {
        super.error(resourceId);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> error(Drawable drawable) {
        super.error(drawable);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> listener(
            RequestListener<? super ModelType, TranscodeType> requestListener) {
        super.listener(requestListener);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> skipMemoryCache(boolean skip) {
        super.skipMemoryCache(skip);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> diskCacheStrategy(DiskCacheStrategy  strategy) {
        super.diskCacheStrategy(strategy);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> override(int width, int height) {
        super.override(width, height);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> thumbnail(
            GenericRequestBuilder<?, ?, ?, TranscodeType> thumbnailRequest) {
        super.thumbnail(thumbnailRequest);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> sourceEncoder(Encoder<ImageVideoWrapper> sourceEncoder) {
        super.sourceEncoder(sourceEncoder);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> dontTransform() {
        super.dontTransform();
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> signature(Key signature) {
        super.signature(signature);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> load(ModelType model) {
        super.load(model);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> clone() {
        return (BitmapRequestBuilder<ModelType, TranscodeType>) super.clone();
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
    public Target<TranscodeType> into(ImageView view) {
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
