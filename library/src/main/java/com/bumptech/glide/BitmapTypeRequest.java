package com.bumptech.glide;

import android.graphics.Bitmap;

import com.bumptech.glide.load.resource.transcode.BitmapBytesTranscoder;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;

/**
 * A class for creating a load request that either loads an {@link Bitmap} directly or that adds an
 * {@link com.bumptech.glide.load.resource.transcode.ResourceTranscoder} to transcode the {@link Bitmap} into another
 * resource type.
 *
 * @param <ModelType> The type of model to load the {@link Bitmap} or transcoded class from.
 */
public class BitmapTypeRequest<ModelType> extends BitmapRequestBuilder<ModelType, Bitmap> {
    private final RequestManager.OptionsApplier optionsApplier;

    BitmapTypeRequest(GenericRequestBuilder<ModelType, ?, ?> other, RequestManager.OptionsApplier optionsApplier) {
        super(Bitmap.class, other);
        this.optionsApplier = optionsApplier;
    }

    /**
     * Sets a transcoder to transcode the decoded and transformed {@link Bitmap} into another resource type.
     *
     * @param transcoder The transoder to use.
     * @param transcodeClass The {@link Class} of the resource the {@link Bitmap} will be transcoded to.
     * @param <R> The type of the resource the {@link Bitmap} will be transcoded to.
     * @return This request builder.
     */
    public <R> BitmapRequestBuilder<ModelType, R> transcode(ResourceTranscoder<Bitmap, R> transcoder,
            Class<R> transcodeClass) {
        return optionsApplier.apply(new BitmapRequestBuilder<ModelType, R>(transcodeClass, this))
                .transcoder(transcoder);
    }

    /**
     * Transcodes the decoded and transformed {@link Bitmap} to bytes by compressing it as a JPEG to a byte array.
     * array.
     *
     * @see #toBytes(android.graphics.Bitmap.CompressFormat, int)
     *
     * @return This request builder.
     */
    public BitmapRequestBuilder<ModelType, byte[]> toBytes() {
        return transcode(new BitmapBytesTranscoder(), byte[].class);
    }

    /**
     * Transcodes the decoded and transformed {@link android.graphics.Bitmap} to bytes by compressing it using the
     * given format and quality to a byte array.
     *
     * @see android.graphics.Bitmap#compress(android.graphics.Bitmap.CompressFormat, int, java.io.OutputStream)
     * @see #toBytes()
     *
     * @param compressFormat The {@link android.graphics.Bitmap.CompressFormat} to use to compress the {@link Bitmap}.
     * @param quality The quality level from 0-100 to use to compress the {@link Bitmap}.
     * @return This request builder.
     */
    public BitmapRequestBuilder<ModelType, byte[]> toBytes(Bitmap.CompressFormat compressFormat, int quality) {
        return transcode(new BitmapBytesTranscoder(compressFormat, quality), byte[].class);
    }
}
