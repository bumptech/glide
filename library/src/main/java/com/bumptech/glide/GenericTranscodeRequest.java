package com.bumptech.glide;

import android.content.Context;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.load.resource.transcode.UnitTranscoder;
import com.bumptech.glide.provider.FixedLoadProvider;
import com.bumptech.glide.provider.LoadProvider;

public class GenericTranscodeRequest<A, T, Z> extends GenericRequestBuilder<A, T, Z, Z>{
    private final Context context;
    private final A model;
    private final Glide glide;
    private final ModelLoader<A, T> modelLoader;
    private final Class<T> dataClass;
    private final Class<Z> resourceClass;

    GenericTranscodeRequest(Context context, Glide glide, A model, ModelLoader<A, T> modelLoader,
            Class<T> dataClass, Class<Z> resourceClass) {
        super(context, model,
                build(glide, modelLoader, dataClass, resourceClass, (ResourceTranscoder<Z, Z>) null),
                resourceClass, glide);
        this.context = context;
        this.model = model;
        this.glide = glide;
        this.modelLoader = modelLoader;
        this.dataClass = dataClass;
        this.resourceClass = resourceClass;
    }

    public <R> GenericRequestBuilder<A, T, Z, R> transcode(ResourceTranscoder<Z, R> transcoder,
            Class<R> transcodeClass) {
        return new GenericRequestBuilder<A, T, Z, R>(context, model, build(glide, modelLoader, dataClass,
                resourceClass, transcoder), transcodeClass, glide);
    }

    private static <A, T, Z, R> LoadProvider<A, T, Z, R> build(Glide glide, ModelLoader<A, T> modelLoader,
            Class<T> dataClass, Class<Z> resourceClass, ResourceTranscoder<Z, R> transcoder) {
        if (transcoder == null) {
            transcoder = UnitTranscoder.get();
        }
        return new FixedLoadProvider<A, T, Z, R>(modelLoader, transcoder,
                glide.buildDataProvider(dataClass, resourceClass));
    }
}
