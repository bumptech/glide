package com.bumptech.glide.request;

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
import com.bumptech.glide.load.data.DataRewinderRegistry;
import com.bumptech.glide.load.engine.Engine;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderRegistry;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.load.resource.transcode.TranscoderRegistry;
import com.bumptech.glide.provider.EncoderRegistry;
import com.bumptech.glide.provider.ResourceDecoderRegistry;
import com.bumptech.glide.provider.ResourceEncoderRegistry;
import com.bumptech.glide.request.target.ImageViewTargetFactory;
import com.bumptech.glide.request.target.Target;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Global context for all loads in Glide containing and exposing the various registries and classes required to load
 * resources.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class GlideContext extends ContextWrapper implements ComponentCallbacks2 {
    private static final String TAG = "RequestContext";

    private final ModelLoaderRegistry modelLoaderRegistry;
    private final EncoderRegistry encoderRegistry;
    private final ResourceDecoderRegistry decoderRegistry;
    private final ResourceEncoderRegistry resultEncoderRegistry;
    private final DataRewinderRegistry dataRewinderRegistry;
    private final TranscoderRegistry transcoderRegistry;
    private final Handler mainHandler;
    private final ImageViewTargetFactory imageViewTargetFactory;
    private final Engine engine;
    private ComponentCallbacks2 componentCallbacks;

    public GlideContext(Context context, ModelLoaderRegistry modelLoaderRegistry, EncoderRegistry encoderRegistry,
            ResourceDecoderRegistry decoderRegistry, ResourceEncoderRegistry resultEncoderRegistry,
            DataRewinderRegistry dataRewinderRegistry, TranscoderRegistry transcoderRegistry,
            ImageViewTargetFactory imageViewTargetFactory, Engine engine, ComponentCallbacks2 componentCallbacks) {
        super(context.getApplicationContext());
        this.modelLoaderRegistry = modelLoaderRegistry;
        this.encoderRegistry = encoderRegistry;
        this.decoderRegistry = decoderRegistry;
        this.resultEncoderRegistry = resultEncoderRegistry;
        this.dataRewinderRegistry = dataRewinderRegistry;
        this.transcoderRegistry = transcoderRegistry;
        this.imageViewTargetFactory = imageViewTargetFactory;
        this.engine = engine;
        this.componentCallbacks = componentCallbacks;

        mainHandler = new Handler(Looper.getMainLooper());
    }

    @SuppressWarnings("unchecked")
    public <X> ResourceEncoder<X> getResultEncoder(Resource<X> resource) throws NoResultEncoderAvailableException {
        ResourceEncoder<X> resourceEncoder = (ResourceEncoder<X>) resultEncoderRegistry.get(resource.get().getClass());
        if (resourceEncoder != null) {
            return resourceEncoder;
        }
        throw new NoResultEncoderAvailableException(resource.get().getClass());
    }

    @SuppressWarnings("unchecked")
    public <X> Encoder<X> getSourceEncoder(X data) throws NoSourceEncoderAvailableException {
        Encoder<X> encoder = encoderRegistry.getEncoder((Class<X>) data.getClass());
        if (encoder != null) {
            return encoder;
        }
        throw new NoSourceEncoderAvailableException(data.getClass());
    }

    public <X> DataRewinder<X> getRewinder(X data) {
        return dataRewinderRegistry.build(data);
    }

    @SuppressWarnings("unchecked")
    public <X, Z> ResourceDecoder<X, Z> getDecoder(DataRewinder<X> rewinder, Class<Z> resourceClass)
            throws NoDecoderAvailableException,
            IOException {
        X data = rewinder.rewindAndGet();
        List<ResourceDecoder<X, Z>> decoders = decoderRegistry.getDecoders((Class<X>) data.getClass(), resourceClass);
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
        DataFetcherSet<?> result = modelLoaderRegistry.getDataFetchers(model, width, height);
        if (result.isEmpty()) {
            throw new NoModelLoaderAvailableException(model);
        }
        return result;
    }

    public <X, Y> ResourceTranscoder<X, Y> getTranscoder(Class<X> resourceClass, Class<Y> transcodeClass) {
        return transcoderRegistry.get(resourceClass, transcodeClass);
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
     * Thrown when no {@link ModelLoader} is registered for a given model class.
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
    public static class NoSourceEncoderAvailableException extends MissingComponentException{
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
