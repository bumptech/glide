package com.bumptech.glide;

import android.content.Context;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.data.transcode.ResourceTranscoder;
import com.bumptech.glide.load.data.transcode.UnitTranscoder;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.provider.FixedLoadProvider;
import com.bumptech.glide.provider.LoadProvider;

public class GenericTranscodeRequest<A, T, Z> extends GenericRequestBuilder<A, T, Z, Z>{
    public GenericTranscodeRequest(Context context, Glide glide, A model, ModelLoader<A, T> modelLoader,
            Class<T> dataClass, Class<Z> resourceClass) {
        super(context, model,
                build(glide, modelLoader, dataClass, resourceClass),
                resourceClass, glide);
    }

    private static <A, T, Z> LoadProvider<A, T, Z, Z> build(Glide glide, ModelLoader<A, T> modelLoader,
            Class<T> dataClass, Class<Z> resourceClass) {
        ResourceTranscoder<Z, Z> transcoder = UnitTranscoder.get();
        DataLoadProvider loadProvider = glide.buildDataProvider(dataClass, resourceClass);
        if (loadProvider == null) {
            loadProvider = new DataLoadProvider() {
                @Override
                public ResourceDecoder getCacheDecoder() {
                    return null;
                }

                @Override
                public ResourceDecoder getSourceDecoder() {
                    return null;
                }

                @Override
                public ResourceEncoder getEncoder() {
                    return null;
                }
            };
        }
        return new FixedLoadProvider<A, T, Z, Z>(modelLoader, transcoder,
                glide.buildDataProvider(dataClass, resourceClass));
    }
}
