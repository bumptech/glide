package com.bumptech.glide;

import android.content.Context;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.load.resource.transcode.UnitTranscoder;
import com.bumptech.glide.manager.Lifecycle;
import com.bumptech.glide.manager.RequestTracker;
import com.bumptech.glide.provider.DataLoadProvider;
import com.bumptech.glide.provider.FixedLoadProvider;
import com.bumptech.glide.provider.LoadProvider;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.target.Target;

import java.io.File;

/**
 * A class for handling requests to load a generic resource type or transcode the generic resource type into another
 * generic resource type.
 *
 * <p>
 *     Warning - It is <em>not</em> safe to use this builder after calling <code>into()</code>, it may be pooled and
 *     reused.
 * </p>
 *
 * @param <ModelType> The type of the model used to retrieve data.
 * @param <DataType> The type of data retrieved.
 * @param <ResourceType> The type of resource to be decoded from the the data.
 */
public class GenericTranscodeRequest<ModelType, DataType, ResourceType>
        extends GenericRequestBuilder<ModelType, DataType, ResourceType, ResourceType> implements DownloadOptions {
    private final ModelLoader<ModelType, DataType> modelLoader;
    private final Class<DataType> dataClass;
    private final Class<ResourceType> resourceClass;
    private final RequestManager.OptionsApplier optionsApplier;

    private static <A, T, Z, R> LoadProvider<A, T, Z, R> build(Glide glide, ModelLoader<A, T> modelLoader,
            Class<T> dataClass, Class<Z> resourceClass, ResourceTranscoder<Z, R> transcoder) {
        DataLoadProvider<T, Z> dataLoadProvider = glide.buildDataProvider(dataClass, resourceClass);
        return new FixedLoadProvider<A, T, Z, R>(modelLoader, transcoder, dataLoadProvider);
    }

    GenericTranscodeRequest(
            Class<ResourceType> transcodeClass, GenericRequestBuilder<ModelType, ?, ?, ?> other,
            ModelLoader<ModelType, DataType> modelLoader, Class<DataType> dataClass, Class<ResourceType> resourceClass,
            RequestManager.OptionsApplier optionsApplier) {
        super(build(other.glide, modelLoader, dataClass, resourceClass, UnitTranscoder.<ResourceType>get()),
                transcodeClass, other);
        this.modelLoader = modelLoader;
        this.dataClass = dataClass;
        this.resourceClass = resourceClass;
        this.optionsApplier = optionsApplier;
    }

    GenericTranscodeRequest(Context context, Glide glide, Class<ModelType> modelClass,
            ModelLoader<ModelType, DataType> modelLoader, Class<DataType> dataClass, Class<ResourceType> resourceClass,
            RequestTracker requestTracker, Lifecycle lifecycle, RequestManager.OptionsApplier optionsApplier) {
        super(context, modelClass, build(glide, modelLoader, dataClass, resourceClass,
                        UnitTranscoder.<ResourceType>get()), resourceClass, glide, requestTracker, lifecycle);
        this.modelLoader = modelLoader;
        this.dataClass = dataClass;
        this.resourceClass = resourceClass;
        this.optionsApplier = optionsApplier;
    }

    /**
     * Adds a transcoder to this request to transcode from the resource type to the given transcode type.
     *
     * @param transcoder The transcoder to use.
     * @param transcodeClass The class of the resource type that will be transcoded to.
     * @param <TranscodeType> The type of the resource that will be transcoded to.
     * @return A new request builder to set options for the transcoded load.
     */
    public <TranscodeType> GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> transcode(
            ResourceTranscoder<ResourceType, TranscodeType> transcoder, Class<TranscodeType> transcodeClass) {
        LoadProvider<ModelType, DataType, ResourceType, TranscodeType> loadProvider = build(glide, modelLoader,
                dataClass, resourceClass, transcoder);

        return optionsApplier.apply(new GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType>(
                loadProvider, transcodeClass, this));
    }

    /**
     * {@inheritDoc}
     */
    public <Y extends Target<File>> Y downloadOnly(Y target) {
        return getDownloadOnlyRequest().into(target);
    }

    /**
     * {@inheritDoc}
     */
    public FutureTarget<File> downloadOnly(int width, int height) {
        return getDownloadOnlyRequest().into(width, height);
    }

    private GenericRequestBuilder<ModelType, DataType, File, File> getDownloadOnlyRequest() {
        ResourceTranscoder<File, File> transcoder = UnitTranscoder.get();
        DataLoadProvider<DataType, File> dataLoadProvider = glide.buildDataProvider(dataClass, File.class);
        FixedLoadProvider<ModelType, DataType, File, File> fixedLoadProvider =
                new FixedLoadProvider<ModelType, DataType, File, File>(modelLoader, transcoder, dataLoadProvider);
        return optionsApplier.apply(new GenericRequestBuilder<ModelType, DataType, File, File>(fixedLoadProvider,
                File.class, this))
                .priority(Priority.LOW)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .skipMemoryCache(true);
    }
}
