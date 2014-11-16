package com.bumptech.glide;

import android.content.Context;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.manager.Lifecycle;
import com.bumptech.glide.manager.RequestTracker;
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
 * @param <ResourceType> The type of resource to be decoded from the the data.
 */
public class GenericTranscodeRequest<ModelType, ResourceType>
        extends GenericRequestBuilder<ModelType, ResourceType, ResourceType> implements DownloadOptions {
    private final Class<ResourceType> resourceClass;
    private final RequestManager.OptionsApplier optionsApplier;

    GenericTranscodeRequest(Class<ResourceType> resourceClass,
            Class<ResourceType> transcodeClass, GenericRequestBuilder<ModelType, ?, ?> other,
             RequestManager.OptionsApplier optionsApplier) {
        super(resourceClass, transcodeClass, other);
        this.resourceClass = resourceClass;
        this.optionsApplier = optionsApplier;
    }

    GenericTranscodeRequest(Context context, Glide glide, Class<ModelType> modelClass,
            Class<ResourceType> resourceClass,
            RequestTracker requestTracker, Lifecycle lifecycle, RequestManager.OptionsApplier optionsApplier) {
        super(context, modelClass, resourceClass, resourceClass, glide, requestTracker, lifecycle);
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
    public <TranscodeType> GenericRequestBuilder<ModelType, ResourceType, TranscodeType> transcode(
            ResourceTranscoder<ResourceType, TranscodeType> transcoder, Class<TranscodeType> transcodeClass) {
        // TODO: fixme.

        return optionsApplier.apply(new GenericRequestBuilder<ModelType, ResourceType, TranscodeType>(
                resourceClass, transcodeClass, this));
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

    private GenericRequestBuilder<ModelType, File, File> getDownloadOnlyRequest() {
        return optionsApplier.apply(new GenericRequestBuilder<ModelType, File, File>(File.class, File.class, this))
                .priority(Priority.LOW)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .skipMemoryCache(true);
    }
}
