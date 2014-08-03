package com.bumptech.glide;

import android.content.Context;

import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.load.resource.transcode.GifDrawableBytesTranscoder;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.manager.Lifecycle;
import com.bumptech.glide.manager.RequestTracker;
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
    private final Context context;
    private final ModelType model;
    private final ModelLoader<ModelType, InputStream> streamModelLoader;
    private final Glide glide;
    private final RequestTracker requestTracker;
    private RequestManager.OptionsApplier optionsApplier;
    private Lifecycle lifecycle;

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

    GifTypeRequest(Context context, ModelType model, ModelLoader<ModelType, InputStream> streamModelLoader, Glide glide,
            RequestTracker requestTracker, Lifecycle lifecycle, RequestManager.OptionsApplier optionsApplier) {
        super(context, model, buildProvider(glide, streamModelLoader, GifDrawable.class, null),  glide,
                requestTracker, lifecycle);
        this.context = context;
        this.model = model;
        this.streamModelLoader = streamModelLoader;
        this.glide = glide;
        this.requestTracker = requestTracker;
        this.optionsApplier = optionsApplier;
        this.lifecycle = lifecycle;

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
        return optionsApplier.apply(model, new GenericRequestBuilder<ModelType, InputStream, GifDrawable, R>(context,
                model, provider, transcodeClass, glide, requestTracker, lifecycle));
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
