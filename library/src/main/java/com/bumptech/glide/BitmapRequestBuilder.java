package com.bumptech.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.ParcelFileDescriptor;
import android.view.animation.Animation;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.Downsampler;
import com.bumptech.glide.load.resource.bitmap.FileDescriptorBitmapDecoder;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.bitmap.ImageVideoBitmapDecoder;
import com.bumptech.glide.load.resource.bitmap.StreamBitmapDecoder;
import com.bumptech.glide.load.resource.bitmap.VideoBitmapDecoder;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.model.ImageVideoWrapper;
import com.bumptech.glide.provider.LoadProvider;
import com.bumptech.glide.request.bitmap.RequestListener;

import java.io.InputStream;

/**
 * A class for creating a request to load a bitmap for an image or from a video. Sets a variety of type independent
 * options including resizing, animations, and placeholders.
 *
 * @param <ModelType> The type of model that will be loaded into the target.
 * @param <TranscodeType> The type of the transcoded resource that the target will receive
 */
@SuppressWarnings("unused") //public api
public class BitmapRequestBuilder<ModelType, TranscodeType> extends GenericRequestBuilder<ModelType, ImageVideoWrapper,
        Bitmap, TranscodeType> {
    private final BitmapPool bitmapPool;
    private Downsampler downsampler = Downsampler.AT_LEAST;
    private DecodeFormat decodeFormat = DecodeFormat.PREFER_RGB_565;
    private ResourceDecoder<InputStream, Bitmap> imageDecoder;
    private ResourceDecoder<ParcelFileDescriptor, Bitmap> videoDecoder;

    BitmapRequestBuilder(Context context, ModelType model,
            LoadProvider<ModelType, ImageVideoWrapper, Bitmap, TranscodeType> streamLoadProvider,
            Class<TranscodeType> transcodeClass, Glide glide) {
        super(context, model, streamLoadProvider, transcodeClass, glide);
        this.bitmapPool = glide.getBitmapPool();

        imageDecoder = new StreamBitmapDecoder(bitmapPool);
        videoDecoder = new FileDescriptorBitmapDecoder(bitmapPool);
    }

    /**
     * Load images at a size near the size of the target using {@link Downsampler#AT_LEAST}.
     *
     * @see #downsample(Downsampler)
     *
     * @return This RequestBuilder
     */
    public BitmapRequestBuilder<ModelType, TranscodeType> approximate() {
        return downsample(Downsampler.AT_LEAST);
    }

    /**
     * Load images at their original size using {@link Downsampler#NONE}.
     *
     * @see #downsample(Downsampler)
     *
     * @return This RequestBuilder
     */
    public BitmapRequestBuilder<ModelType, TranscodeType> asIs() {
        return downsample(Downsampler.NONE);
    }

    /**
     * Load images using the given {@link Downsampler}. Replaces any existing image decoder. Defaults to
     * {@link Downsampler#AT_LEAST}. Will be ignored if the data represented by the model is a video. This replaces any
     * previous calls to {@link #imageDecoder(ResourceDecoder)}  and {@link #decoder(ResourceDecoder)} with default
     * decoders with the appropriate options set.
     *
     * @see #imageDecoder
     *
     * @param downsampler The downsampler
     * @return This RequestBuilder
     */
    private BitmapRequestBuilder<ModelType, TranscodeType> downsample(Downsampler downsampler) {
        this.downsampler = downsampler;
        imageDecoder = new StreamBitmapDecoder(downsampler, bitmapPool, decodeFormat);
        super.decoder(new ImageVideoBitmapDecoder(imageDecoder, videoDecoder));
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> thumbnail(float sizeMultiplier) {
        super.thumbnail(sizeMultiplier);
        return this;
    }

    public BitmapRequestBuilder<ModelType, TranscodeType> thumbnail(BitmapRequestBuilder<ModelType, TranscodeType>
            thumbnailRequest) {
        super.thumbnail(thumbnailRequest);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> sizeMultiplier(float sizeMultiplier) {
        super.sizeMultiplier(sizeMultiplier);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> decoder(ResourceDecoder<ImageVideoWrapper, Bitmap> decoder) {
        super.decoder(decoder);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> cacheDecoder(
            ResourceDecoder<InputStream, Bitmap> cacheDecoder) {
        super.cacheDecoder(cacheDecoder);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> encoder(ResourceEncoder<Bitmap> encoder) {
        super.encoder(encoder);
        return this;
    }

    public BitmapRequestBuilder<ModelType, TranscodeType> imageDecoder(ResourceDecoder<InputStream, Bitmap> decoder) {
        imageDecoder = decoder;
        super.decoder(new ImageVideoBitmapDecoder(decoder, videoDecoder));
        return this;
    }

    public BitmapRequestBuilder<ModelType, TranscodeType> videoDecoder(
            ResourceDecoder<ParcelFileDescriptor, Bitmap> decoder) {
        videoDecoder = decoder;
        super.decoder(new ImageVideoBitmapDecoder(imageDecoder, decoder));
        return this;
    }

    /**
     * Sets the preferred format for {@link Bitmap}s decoded in this request. Defaults to
     * {@link DecodeFormat#PREFER_RGB_565}. This replaces any previous calls to {@link #imageDecoder(ResourceDecoder)},
     * {@link #videoDecoder(ResourceDecoder)} and {@link #decoder(ResourceDecoder)} with default decoders with the
     * appropriate options set.
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
        super.decoder(new ImageVideoBitmapDecoder(imageDecoder, videoDecoder));
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> priority(Priority priority) {
        super.priority(priority);
        return this;
    }

    /**
     * Transform images using {@link CenterCrop}.
     *
     * @return This RequestBuilder
     */
    public BitmapRequestBuilder<ModelType, TranscodeType> centerCrop() {
        return transform(new CenterCrop(bitmapPool));
    }

    /**
     * Transform images using {@link FitCenter}.
     *
     * @return This RequestBuilder
     */
    public BitmapRequestBuilder<ModelType, TranscodeType> fitCenter() {
        return transform(new FitCenter(bitmapPool));
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> transform(Transformation<Bitmap> transformation) {
        super.transform(transformation);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> transcoder(
            ResourceTranscoder<Bitmap, TranscodeType> transcoder) {
        super.transcoder(transcoder);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> animate(int animationId) {
        super.animate(animationId);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> animate(Animation animation) {
        super.animate(animation);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> placeholder(int resourceId) {
        super.placeholder(resourceId);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> placeholder(Drawable drawable) {
        super.placeholder(drawable);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> error(int resourceId) {
        super.error(resourceId);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> error(Drawable drawable) {
        super.error(drawable);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> listener(RequestListener<ModelType> requestListener) {
        super.listener(requestListener);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> skipMemoryCache(boolean skip) {
        super.skipMemoryCache(skip);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> skipDiskCache(boolean skip) {
        super.skipDiskCache(skip);
        return this;
    }

    @Override
    public BitmapRequestBuilder<ModelType, TranscodeType> skipCache(boolean skip) {
        super.skipCache(skip);
        return this;
    }
}
