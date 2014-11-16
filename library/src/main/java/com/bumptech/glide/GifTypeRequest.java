package com.bumptech.glide;

import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.load.resource.transcode.GifDrawableBytesTranscoder;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;

/**
 * A class for creating a load request that either loads an {@link com.bumptech.glide.load.resource.gif.GifDrawable}
 * directly or that adds an {@link com.bumptech.glide.load.resource.transcode.ResourceTranscoder} to transcode
 * {@link com.bumptech.glide.load.resource.gif.GifDrawable} into another resource type.
 *
 * @param <ModelType> The type of model to load the {@link com.bumptech.glide.load.resource.gif.GifDrawable} or other
 *           transcoded class from.
 */
public class GifTypeRequest<ModelType> extends GifRequestBuilder<ModelType> {
    private final RequestManager.OptionsApplier optionsApplier;

    GifTypeRequest(GenericRequestBuilder<ModelType, ?, ?> other, RequestManager.OptionsApplier optionsApplier) {
        super(other);
        this.optionsApplier = optionsApplier;

        // Default to animating.
        crossFade();
    }

    /**
     * Sets a transcoder to transcode the decoded {@link com.bumptech.glide.load.resource.gif.GifDrawable} into another
     * resource type.
     *
     * @param transcoder The transcoder to use.
     * @param transcodeClass The {@link Class} of the resource the
     * {@link com.bumptech.glide.load.resource.gif.GifDrawable} will be transcoded to.
     *
     * @param <R> The type of the resource the {@link com.bumptech.glide.load.resource.gif.GifDrawable} will be
     *           trasncoded to.
     * @return This request builder.
     */
    public <R> GenericRequestBuilder<ModelType, GifDrawable, R> transcode(
            ResourceTranscoder<GifDrawable, R> transcoder, Class<R> transcodeClass) {
        // TODO: fixme.
        return optionsApplier.apply(new GenericRequestBuilder<ModelType, GifDrawable, R>(
                GifDrawable.class, transcodeClass, this));
    }

    /**
     * Setup the request to return the bytes of the loaded gif.
     * <p>
     *     Note - Any transformations added during this load do not change the underlying bytes and therefore this
     *     will always load and provide the bytes of the original image before any transformations to the given target.
     * </p>
     *
     * @return A new Builder object to build a request to transform the given model into the bytes of an animated gif.
     */
    public GenericRequestBuilder<ModelType, GifDrawable, byte[]> toBytes() {
        return transcode(new GifDrawableBytesTranscoder(), byte[].class);
    }
}
