package com.bumptech.glide.load.engine;

import android.content.ContextWrapper;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.data.DataFetcherSet;
import com.bumptech.glide.load.data.DataRewinder;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.request.GlideContext;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.util.Util;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Context for individual requests and decodes that contains and exposes classes necessary to obtain, decode, and
 * encode resources.
 *
 * @param <ResourceClass> The type of objects decoded using classes from this object.
 * @param <TranscodeClass> The type of resources returned using classes from this object.
 */
public class RequestContext<ResourceClass, TranscodeClass> extends ContextWrapper {

    private final GlideContext glideContext;
    private final Object model;
    private final Class<ResourceClass> resourceClass;
    private final Class<TranscodeClass> transcodeClass;
    private final Transformation<ResourceClass> transformation;
    private final ResourceTranscoder<ResourceClass, ? extends TranscodeClass> transcoder;
    private final RequestOptions requestOptions;

    private DataFetcherSet<?> fetchers;

    public RequestContext(GlideContext glideContext, Object model, Class<ResourceClass> resourceClass,
            Class<TranscodeClass> transcodeClass, Transformation<ResourceClass> transformation,
            ResourceTranscoder<ResourceClass, ? extends TranscodeClass> transcoder, RequestOptions requestOptions) {
        super(glideContext);
        this.glideContext = glideContext;
        this.model = model;
        this.resourceClass = resourceClass;
        this.transcodeClass = transcodeClass;
        this.transformation = transformation;
        this.transcoder = transcoder;
        this.requestOptions = requestOptions;
    }

    List<LoadPath<?, ResourceClass, TranscodeClass>> getLoadPaths() {
        return glideContext.getLoadPaths(model, resourceClass, transcodeClass);
    }

    List<LoadPath<?, ResourceClass, TranscodeClass>> getSourceCacheLoadPaths(File sourceCacheFile) {
        return glideContext.getLoadPaths(sourceCacheFile, resourceClass, transcodeClass);
    }

    void buildDataFetchers(int width, int height) {
        Util.assertMainThread();
        if (fetchers == null) {
            fetchers = glideContext.getDataFetchers(model, width, height);
        }
    }

    DataFetcherSet<?> getDataFetchers() {
        if (fetchers == null) {
            throw new IllegalStateException("Must call buildDataFetchers first");
        }
        return fetchers;
    }

    String getId() {
        if (fetchers == null) {
            throw new IllegalStateException("Must call buildDataFetchers first");
        }
        return fetchers.getId();
    }

    Key getSignature() {
        return requestOptions.getSignature();
    }

    Class<ResourceClass> getResourceClass() {
        return resourceClass;
    }

    Class<TranscodeClass> getTranscodeClass() {
        return transcodeClass;
    }

    Transformation<ResourceClass> getTransformation() {
        return transformation;
    }

    boolean isMemoryCacheable() {
        return requestOptions.isMemoryCacheable();
    }

    DiskCacheStrategy getDiskCacheStrategy() {
        return requestOptions.getDiskCacheStrategy();
    }

    Priority getPriority() {
        return requestOptions.getPriority();
    }

    ResourceTranscoder<ResourceClass, ? extends TranscodeClass> getTranscoder() {
        return transcoder != null ? transcoder : glideContext.getTranscoder(resourceClass, transcodeClass);
    }

    <X> DataRewinder<X> getRewinder(X data) {
        return glideContext.getRewinder(data);
    }

    <X> ResourceDecoder<X, ResourceClass> getDecoder(DataRewinder<X> rewinder)
            throws IOException, GlideContext.NoDecoderAvailableException {
        return glideContext.getDecoder(rewinder, resourceClass);
    }

    ResourceEncoder<ResourceClass> getResultEncoder(Resource<ResourceClass> resource)
            throws GlideContext.NoResultEncoderAvailableException {
        return glideContext.getResultEncoder(resource);
    }

    <X> Encoder<X> getSourceEncoder(X data) throws GlideContext.NoSourceEncoderAvailableException {
        return glideContext.getSourceEncoder(data);
    }

    DataFetcherSet<?> getDataFetchers(File file, int width, int height) throws GlideContext.NoModelLoaderAvailableException {
        return glideContext.getDataFetchers(file, width, height);
    }
}
