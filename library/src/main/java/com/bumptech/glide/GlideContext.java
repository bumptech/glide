package com.bumptech.glide;

import android.annotation.TargetApi;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.data.DataFetcherSet;
import com.bumptech.glide.load.data.DataRewinder;
import com.bumptech.glide.load.engine.DecodeOptions;
import com.bumptech.glide.load.engine.DecodePath;
import com.bumptech.glide.load.engine.Engine;
import com.bumptech.glide.load.engine.LoadPath;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.request.target.ImageViewTargetFactory;
import com.bumptech.glide.request.target.Target;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Global context for all loads in Glide containing and exposing the various registries and classes required to load
 * resources.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class GlideContext extends ContextWrapper implements ComponentCallbacks2 {
    private static final String TAG = "RequestContext";

    private final Handler mainHandler;
    private final Registry registry;
    private final ImageViewTargetFactory imageViewTargetFactory;
    private final DecodeOptions decodeOptions;
    private final Engine engine;
    private final ComponentCallbacks2 componentCallbacks;

    public GlideContext(Context context, Registry registry, ImageViewTargetFactory imageViewTargetFactory,
            DecodeOptions decodeOptions, Engine engine, ComponentCallbacks2 componentCallbacks) {
        super(context.getApplicationContext());
        this.registry = registry;
        this.imageViewTargetFactory = imageViewTargetFactory;
        this.decodeOptions = decodeOptions;
        this.engine = engine;
        this.componentCallbacks = componentCallbacks;

        mainHandler = new Handler(Looper.getMainLooper());
    }

    public DecodeOptions getDecodeOptions() {
        return decodeOptions;
    }

    public <ResourceType, TranscodeType> List<LoadPath<?, ResourceType, TranscodeType>> getLoadPaths(Object model,
            Class<ResourceType> resourceClass, Class<TranscodeType> transcodeClass) {
        Class<?> modelClass = model.getClass();
        List<Class<?>> dataClasses = registry.modelLoaderRegistry.getDataClasses(modelClass);
        List<LoadPath<?, ResourceType, TranscodeType>> loadPaths =
                new ArrayList<>();
        for (Class<?> dataClass : dataClasses) {
            LoadPath<?, ResourceType, TranscodeType> path = getLoadPath(dataClass, resourceClass, transcodeClass);
            if (path != null) {
                loadPaths.add(path);
            }
        }
        if (loadPaths.isEmpty()) {
            throw new IllegalArgumentException("No load path found for path " + modelClass + "->" + resourceClass
                    + "->" + transcodeClass);
        }
        return loadPaths;
    }

    private <DataType, ResourceType, TranscodeType>
            LoadPath<DataType, ResourceType, TranscodeType> getLoadPath(Class<DataType> dataClass,
            Class<ResourceType> resourceClass, Class<TranscodeType> transcodeClass) {

        List<DecodePath<DataType, ResourceType, TranscodeType>> decodePaths =
                getDecodePaths(dataClass, resourceClass, transcodeClass);
        // It's possible there is no way to decode or transcode to the desired types from a given data class.
        if (!decodePaths.isEmpty()) {
            return new LoadPath<>(dataClass, resourceClass, transcodeClass,
                    decodePaths);
        } else {
            return null;
        }
    }

    private <DataType, ResourceType, TranscodeType> List<DecodePath<DataType, ResourceType, TranscodeType>>
            getDecodePaths(Class<DataType> dataClass, Class<ResourceType> resourceClass,
            Class<TranscodeType> transcodeClass) {
        List<DecodePath<DataType, ResourceType, TranscodeType>> decodePaths =
                new ArrayList<>();
        List<Class<ResourceType>> registeredResourceClasses =
                registry.decoderRegistry.getResourceClasses(dataClass, resourceClass);
        for (Class<ResourceType> registeredResourceClass : registeredResourceClasses) {
            List<Class<TranscodeType>> registeredTranscodeClasses =
                    registry.transcoderRegistry.getTranscodeClasses(registeredResourceClass, transcodeClass);
            for (Class<TranscodeType> registeredTranscodeClass : registeredTranscodeClasses) {
                List<ResourceDecoder<DataType, ResourceType>> decoders =
                        registry.decoderRegistry.getDecoders(dataClass, registeredResourceClass);
                ResourceTranscoder<ResourceType, TranscodeType> transcoder =
                        registry.transcoderRegistry.get(registeredResourceClass, registeredTranscodeClass);
                decodePaths.add(new DecodePath<>(dataClass,
                        registeredResourceClass, registeredTranscodeClass, decoders, transcoder));
            }
        }
        return decodePaths;
    }

    public List<Class<?>> getRegisteredResourceClasses(Class<?> modelClass, Class<?> resourceClass) {
        List<Class<?>> result = new ArrayList<>();
        List<Class<?>> dataClasses = registry.modelLoaderRegistry.getDataClasses(modelClass);
        for (Class<?> dataClass : dataClasses) {
            List<? extends Class<?>> registeredResourceClasses =
                    registry.decoderRegistry.getResourceClasses(dataClass, resourceClass);
            result.addAll(registeredResourceClasses);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public <X> ResourceEncoder<X> getResultEncoder(Resource<X> resource) throws NoResultEncoderAvailableException {
        ResourceEncoder<X> resourceEncoder = (ResourceEncoder<X>) registry.resourceEncoderRegistry.get(
                resource.get().getClass());
        if (resourceEncoder != null) {
            return resourceEncoder;
        }
        throw new NoResultEncoderAvailableException(resource.get().getClass());
    }

    @SuppressWarnings("unchecked")
    public <X> Encoder<X> getSourceEncoder(X data) throws NoSourceEncoderAvailableException {
        Encoder<X> encoder = registry.encoderRegistry.getEncoder((Class<X>) data.getClass());
        if (encoder != null) {
            return encoder;
        }
        throw new NoSourceEncoderAvailableException(data.getClass());
    }

    public <X> DataRewinder<X> getRewinder(X data) {
        return registry.dataRewinderRegistry.build(data);
    }

    @SuppressWarnings("unchecked")
    public <X, Z> ResourceDecoder<X, Z> getDecoder(DataRewinder<X> rewinder, Class<Z> resourceClass)
            throws NoDecoderAvailableException,
            IOException {
        X data = rewinder.rewindAndGet();
        List<ResourceDecoder<X, Z>> decoders =
                registry.decoderRegistry.getDecoders((Class<X>) data.getClass(), resourceClass);
        for (ResourceDecoder<X, Z> decoder : decoders) {
            if (decoder.handles(data)) {
                maybeLogFoundDecoder(decoders, decoder);
                return decoder;
            }
            data = rewinder.rewindAndGet();
        }
        throw new NoDecoderAvailableException(data.getClass(), resourceClass);
    }

    private static <X, Z> void maybeLogFoundDecoder(List<ResourceDecoder<X, Z>> decoders,
            ResourceDecoder<X, Z> handles) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Found decoder: " + handles + " from "
                    + Arrays.toString(decoders.toArray(new Object[decoders.size()])));
        }
    }

    public DataFetcherSet<?> getDataFetchers(Object model, int width, int height) {
        DataFetcherSet<?> result = registry.modelLoaderRegistry.getDataFetchers(model, width, height);
        if (result.isEmpty()) {
            throw new NoModelLoaderAvailableException(model);
        }
        return result;
    }

    public <X, Y> ResourceTranscoder<X, Y> getTranscoder(Class<X> resourceClass, Class<Y> transcodeClass) {
        return registry.transcoderRegistry.get(resourceClass, transcodeClass);
    }

    public <X> Target<X> buildImageViewTarget(ImageView imageView, Class<X> transcodeClass) {
        return imageViewTargetFactory.buildTarget(imageView, transcodeClass);
    }

    public Handler getMainHandler() {
        return mainHandler;
    }

    public Engine getEngine() {
        return engine;
    }

    @Override
    public void onTrimMemory(int level) {
        componentCallbacks.onTrimMemory(level);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        componentCallbacks.onConfigurationChanged(newConfig);
    }

    @Override
    public void onLowMemory() {
        componentCallbacks.onLowMemory();
    }

    /**
     * Thrown when no {@link com.bumptech.glide.load.model.ModelLoader} is registered for a given model class.
     */
    public static class NoModelLoaderAvailableException extends RuntimeException {
        public NoModelLoaderAvailableException(Object model) {
            super("Failed to find any ModelLoaders for model: " + model);
        }
    }

    /**
     * Thrown when no {@link ResourceEncoder} is registered for a given resource class.
     */
    public static class NoResultEncoderAvailableException extends MissingComponentException {
        public NoResultEncoderAvailableException(Class<?> resourceClass) {
            super("Failed to find result encoder for resource class: " + resourceClass);
        }
    }

    /**
     * Thrown when no {@link Encoder} is registered for a given data class.
     */
    public static class NoSourceEncoderAvailableException extends MissingComponentException {
        public NoSourceEncoderAvailableException(Class<?> dataClass) {
            super("Failed to find source encoder for data class: " + dataClass);
        }
    }

    /**
     * Thrown when no {@link ResourceDecoder} is registered for a given data and resource class.
     */
    public static class NoDecoderAvailableException extends MissingComponentException {
        public NoDecoderAvailableException(Class<?> dataClass, Class<?> resourceClass) {
            super("Failed to find decoder that decodes " + resourceClass + " from data " + dataClass);
        }
    }

    /**
     * Thrown when some necessary component is missing for a load.
     */
    public static class MissingComponentException extends RuntimeException {
        public MissingComponentException(String message) {
            super(message);
        }
    }
}
