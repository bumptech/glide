package com.bumptech.glide;

import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.load.resource.transcode.GifDrawableBytesTranscoder;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.provider.DataLoadProvider;
import com.bumptech.glide.provider.FixedLoadProvider;

import java.io.InputStream;

/**
 * A class for creating a load request that either loads an {@link com.bumptech.glide.load.resource.gif.GifDrawable}
 * directly or that adds an {@link com.bumptech.glide.load.resource.transcode.ResourceTranscoder} to transcode
 * {@link com.bumptech.glide.load.resource.gif.GifDrawable} into another resource type.
 *
 * @param <ModelType> The type of model to load the {@link com.bumptech.glide.load.resource.gif.GifDrawable} or other
 *           transcoded class from.
 */
public class GifTypeRequest<ModelType> extends GifRequestBuilder<ModelType> {
    private final ModelLoader<ModelType, InputStream> streamModelLoader;
    private final RequestManager.OptionsApplier optionsApplier;

    private static <A, R> FixedLoadProvider<A, InputStream, GifDrawable, R> buildProvider(Glide glide,
            ModelLoader<A, InputStream> streamModelLoader, Class<R> transcodeClass,
            ResourceTranscoder<GifDrawable, R> transcoder) {
        if (streamModelLoader == null) {
            return null;
        }

        if (transcoder == null) {
            transcoder = glide.buildTranscoder(GifDrawable.class, transcodeClass);
        }
        DataLoadProvider<InputStream, GifDrawable> dataLoadProvider = glide.buildDataProvider(InputStream.class,
                GifDrawable.class);
        return new FixedLoadProvider<A, InputStream, GifDrawable, R>(streamModelLoader, transcoder, dataLoadProvider);
    }

    GifTypeRequest(GenericRequestBuilder<ModelType, ?, ?, ?> other,
            ModelLoader<ModelType, InputStream> streamModelLoader, RequestManager.OptionsApplier optionsApplier) {
        super(buildProvider(other.glide, streamModelLoader, GifDrawable.class, null), GifDrawable.class, other);
        this.streamModelLoader = streamModelLoader;
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
    public <R> GenericRequestBuilder<ModelType, InputStream, GifDrawable, R> transcode(
            ResourceTranscoder<GifDrawable, R> transcoder, Class<R> transcodeClass) {
        FixedLoadProvider<ModelType, InputStream, GifDrawable, R> provider = buildProvider(glide, streamModelLoader,
                transcodeClass, transcoder);
        return optionsApplier.apply(new GenericRequestBuilder<ModelType, InputStream, GifDrawable, R>(provider,
                transcodeClass, this));
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
    public GenericRequestBuilder<ModelType, InputStream, GifDrawable, byte[]> toBytes() {
        return transcode(new GifDrawableBytesTranscoder(), byte[].class);
    }
}
