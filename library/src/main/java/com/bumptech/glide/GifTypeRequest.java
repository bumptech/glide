package com.bumptech.glide;

import android.content.Context;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.resource.gif.GifData;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.load.resource.transcode.GifDataBytesTranscoder;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.manager.RequestManager;
import com.bumptech.glide.provider.FixedLoadProvider;

import java.io.InputStream;

public class GifTypeRequest<A> extends GifRequestBuilder<A, GifDrawable> {
    private final Context context;
    private final A model;
    private final ModelLoader<A, InputStream> streamModelLoader;
    private final Glide glide;
    private RequestManager requestManager;

    private static <A, R> FixedLoadProvider<A, InputStream, GifData, R> buildProvider(Glide glide,
            ModelLoader<A, InputStream> streamModelLoader, Class<R> transcodeClass,
            ResourceTranscoder<GifData, R> transcoder) {
        if (transcoder == null) {
            transcoder = glide.buildTranscoder(GifData.class, transcodeClass);
        }
        return streamModelLoader == null ? null :
                new FixedLoadProvider<A, InputStream, GifData, R>(streamModelLoader, transcoder,
                        glide.buildDataProvider(InputStream.class, GifData.class));

    }

    GifTypeRequest(Context context, A model, ModelLoader<A, InputStream> streamModelLoader, Glide glide,
            RequestManager requestManager){
        super(context, model, buildProvider(glide, streamModelLoader, GifDrawable.class, null), GifDrawable.class,
                glide, requestManager);
        this.context = context;
        this.model = model;
        this.streamModelLoader = streamModelLoader;
        this.glide = glide;
        this.requestManager = requestManager;
    }

    public <R> GifRequestBuilder<A, R> transcode(ResourceTranscoder<GifData, R> transcoder, Class<R> transcodeClass) {
        return new GifRequestBuilder<A, R>(context, model,
                buildProvider(glide, streamModelLoader, transcodeClass, transcoder), transcodeClass, glide,
                requestManager);
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
