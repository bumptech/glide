package com.bumptech.glide;

import android.content.Context;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.load.resource.transcode.UnitTranscoder;
import com.bumptech.glide.manager.RequestTracker;
import com.bumptech.glide.provider.FixedLoadProvider;
import com.bumptech.glide.provider.LoadProvider;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.target.Target;

import java.io.File;

public class GenericTranscodeRequest<A, T, Z> extends GenericRequestBuilder<A, T, Z, Z>{
    private final Context context;
    private final A model;
    private final Glide glide;
    private final ModelLoader<A, T> modelLoader;
    private final Class<T> dataClass;
    private final Class<Z> resourceClass;
    private final RequestTracker requestTracker;
    private final RequestManager.OptionsApplier optionsApplier;

    private static <A, T, Z, R> LoadProvider<A, T, Z, R> build(Glide glide, ModelLoader<A, T> modelLoader,
            Class<T> dataClass, Class<Z> resourceClass, ResourceTranscoder<Z, R> transcoder) {
        if (transcoder == null) {
            transcoder = UnitTranscoder.get();
        }
        DataLoadProvider<T, Z> dataLoadProvider = glide.buildDataProvider(dataClass, resourceClass);
        return new FixedLoadProvider<A, T, Z, R>(modelLoader, transcoder, dataLoadProvider);
    }

    GenericTranscodeRequest(Context context, Glide glide, A model, ModelLoader<A, T> modelLoader,
            Class<T> dataClass, Class<Z> resourceClass, RequestTracker requestTracker,
            RequestManager.OptionsApplier optionsApplier) {
        super(context, model,
                build(glide, modelLoader, dataClass, resourceClass, (ResourceTranscoder<Z, Z>) null),
                resourceClass, glide, requestTracker);
        this.context = context;
        this.model = model;
        this.glide = glide;
        this.modelLoader = modelLoader;
        this.dataClass = dataClass;
        this.resourceClass = resourceClass;
        this.requestTracker = requestTracker;
        this.optionsApplier = optionsApplier;
    }

    public <R> GenericRequestBuilder<A, T, Z, R> transcode(ResourceTranscoder<Z, R> transcoder,
            Class<R> transcodeClass) {
        return optionsApplier.apply(model, new GenericRequestBuilder<A, T, Z, R>(context, model,
                build(glide, modelLoader, dataClass, resourceClass, transcoder), transcodeClass, glide,
                requestTracker));
    }

    /**
     * Loads the original unmodified data into the cache and calls the given Target with the cache File
     * @param target The Target that will receive the cache File when the load completes
     * @param <Y> The type of Target.
     * @return The given Target.
     */
    public <Y extends Target<File>> Y downloadOnly(Y target) {
        return getDownloadOnlyRequest().into(target);
    }

    /**
     * Loads the original unmodified data into the cache and returns a {@link java.util.concurrent.Future} that can be
     * used to retrieve the cache File containing the data.
     * @param width The width to use to fetch the data.
     * @param height The height to use to fetch the data.
     * @return A {@link java.util.concurrent.Future} that can be used to retrieve the cache File containing the data.
     */
    public FutureTarget<File> downloadOnly(int width, int height) {
        return getDownloadOnlyRequest().into(width, height);
    }

    private GenericRequestBuilder<A, T, File, File> getDownloadOnlyRequest() {
        ResourceTranscoder<File, File> transcoder = UnitTranscoder.get();
        DataLoadProvider<T, File> dataLoadProvider = glide.buildDataProvider(dataClass, File.class);
        FixedLoadProvider<A, T, File, File> fixedLoadProvider = new FixedLoadProvider<A, T, File, File>(modelLoader,
                transcoder, dataLoadProvider);
        return optionsApplier.apply(model, new GenericRequestBuilder<A, T, File, File>(context, model,
                fixedLoadProvider,
                File.class, glide,
                requestTracker)
                .priority(Priority.LOW)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .skipMemoryCache(true));

    }
}
