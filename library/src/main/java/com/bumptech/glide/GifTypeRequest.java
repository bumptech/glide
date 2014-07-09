package com.bumptech.glide;

import android.content.Context;

import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.resource.gif.GifData;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.load.resource.transcode.GifDataBytesTranscoder;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.manager.RequestTracker;
import com.bumptech.glide.provider.DataLoadProvider;
import com.bumptech.glide.provider.FixedLoadProvider;

import java.io.InputStream;

/**
 * A class for creating a load request that either loads an {@link com.bumptech.glide.load.resource.gif.GifDrawable}
 * directly or that adds an {@link com.bumptech.glide.load.resource.transcode.ResourceTranscoder} to transcode
 * {@link com.bumptech.glide.load.resource.gif.GifData} into another resource type.
 *
 * @param <A> The type of model to load the {@link com.bumptech.glide.load.resource.gif.GifDrawable} or other
 *           transcoded class from.
 */
public class GifTypeRequest<A> extends GifRequestBuilder<A, GifDrawable> {
    private final Context context;
    private final A model;
    private final ModelLoader<A, InputStream> streamModelLoader;
    private final Glide glide;
    private final RequestTracker requestTracker;
    private RequestManager.OptionsApplier optionsApplier;

    private static <A, R> FixedLoadProvider<A, InputStream, GifData, R> buildProvider(Glide glide,
            ModelLoader<A, InputStream> streamModelLoader, Class<R> transcodeClass,
            ResourceTranscoder<GifData, R> transcoder) {
        if (streamModelLoader == null) {
            return null;
        }

        if (transcoder == null) {
            transcoder = glide.buildTranscoder(GifData.class, transcodeClass);
        }
        DataLoadProvider<InputStream, GifData> dataLoadProvider = glide.buildDataProvider(InputStream.class,
                GifData.class);
        return new FixedLoadProvider<A, InputStream, GifData, R>(streamModelLoader, transcoder, dataLoadProvider);

    }

    GifTypeRequest(Context context, A model, ModelLoader<A, InputStream> streamModelLoader, Glide glide,
            RequestTracker requestTracker, RequestManager.OptionsApplier optionsApplier) {
        super(context, model, buildProvider(glide, streamModelLoader, GifDrawable.class, null), GifDrawable.class,
                glide, requestTracker);
        this.context = context;
        this.model = model;
        this.streamModelLoader = streamModelLoader;
        this.glide = glide;
        this.requestTracker = requestTracker;
        this.optionsApplier = optionsApplier;
    }

    /**
     * Sets a transcoder to transcode the decoded {@link com.bumptech.glide.load.resource.gif.GifData} into another
     * resource type.
     *
     * @param transcoder The transcoder to use.
     * @param transcodeClass The {@link Class} of the resource the
     * {@link com.bumptech.glide.load.resource.gif.GifData} will be transcoded to.
     *
     * @param <R> The type of the resource the {@link com.bumptech.glide.load.resource.gif.GifData} will be
     *           trasncoded to.
     * @return This request builder.
     */
    public <R> GifRequestBuilder<A, R> transcode(ResourceTranscoder<GifData, R> transcoder, Class<R> transcodeClass) {
        return optionsApplier.apply(model, new GifRequestBuilder<A, R>(context, model,
                buildProvider(glide, streamModelLoader, transcodeClass, transcoder), transcodeClass, glide,
                requestTracker));
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
    public GifRequestBuilder<A, byte[]> toBytes() {
        return transcode(new GifDataBytesTranscoder(), byte[].class);
    }
}
