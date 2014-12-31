package com.bumptech.glide;

import android.content.Context;

import com.bumptech.glide.manager.Lifecycle;
import com.bumptech.glide.manager.RequestTracker;

/**
 * A class for handling requests to load a generic resource type or transcode the generic resource type into another
 * generic resource type.
 *
 * <p>
 *     Warning - It is <em>not</em> safe to use this builder after calling <code>into()</code>, it may be pooled and
 *     reused.
 * </p>
 *
 * @param <ResourceType> The type of resource to be decoded from the the data.
 */
public final class TranscodeRequest<ResourceType>
        extends RequestBuilder<ResourceType, ResourceType> {
    private final Class<ResourceType> resourceClass;

    TranscodeRequest(Class<ResourceType> resourceClass, RequestBuilder<?, ?> other) {
        super(resourceClass, resourceClass, other);
        this.resourceClass = resourceClass;
    }

    TranscodeRequest(Context context, Glide glide, Class<ResourceType> resourceClass, RequestTracker requestTracker,
            Lifecycle lifecycle) {
        super(context, resourceClass, resourceClass, glide, requestTracker, lifecycle);
        this.resourceClass = resourceClass;
    }

    /**
     * Adds a transcoder to this request to transcode from the resource type to the given transcode type.
     *
     * @param transcodeClass The class of the resource type that will be transcoded to.
     * @param <TranscodeType> The type of the resource that will be transcoded to.
     * @return A new request builder to set options for the transcoded load.
     */
    public <TranscodeType> RequestBuilder<ResourceType, TranscodeType> to(Class<TranscodeType> transcodeClass) {
        return new RequestBuilder<ResourceType, TranscodeType>(
                resourceClass, transcodeClass, this);
    }
}
