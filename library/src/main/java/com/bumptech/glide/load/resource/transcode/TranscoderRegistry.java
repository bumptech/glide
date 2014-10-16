package com.bumptech.glide.load.resource.transcode;

import com.bumptech.glide.util.MultiClassKey;

import java.util.HashMap;
import java.util.Map;

/**
 * A class that allows {@link com.bumptech.glide.load.resource.transcode.ResourceTranscoder}s to be registered and
 * retrieved by the classes they convert between.
 */
public class TranscoderRegistry {
    private static final MultiClassKey GET_KEY = new MultiClassKey();

    private final Map<MultiClassKey, ResourceTranscoder<?, ?>> factories =
            new HashMap<MultiClassKey, ResourceTranscoder<?, ?>>();

    /**
     * Registers the given {@link com.bumptech.glide.load.resource.transcode.ResourceTranscoder} using the given
     * classes so it can later be retrieved using the given classes.
     *
     * @param decodedClass The class of the resource that the transcoder transcodes from.
     * @param transcodedClass The class of the resource that the transcoder transcodes to.
     * @param transcoder The transcoder.
     * @param <Z> The type of the resource that the transcoder transcodes from.
     * @param <R> The type of the resource that the transcoder transcodes to.
     */
    public <Z, R> void register(Class<Z> decodedClass, Class<R> transcodedClass, ResourceTranscoder<Z, R> transcoder) {
        factories.put(new MultiClassKey(decodedClass, transcodedClass), transcoder);
    }

    /**
     * Returns the currently registered {@link com.bumptech.glide.load.resource.transcode.ResourceTranscoder} for the
     * given classes.
     *
     * @param decodedClass The class of the resource that the transcoder transcodes from.
     * @param transcodedClass The class of the resource that the transcoder transcodes to.
     * @param <Z> The type of the resource that the transcoder transcodes from.
     * @param <R> The type of the resource that the transcoder transcodes to.
     */
    @SuppressWarnings("unchecked")
    public <Z, R> ResourceTranscoder<Z, R> get(Class<Z> decodedClass, Class<R> transcodedClass) {
        if (decodedClass.equals(transcodedClass)) {
            // we know they're the same type (Z and R)
            return (ResourceTranscoder<Z, R>) UnitTranscoder.get();
        }
        final ResourceTranscoder<?, ?> result;
        synchronized (GET_KEY) {
            GET_KEY.set(decodedClass, transcodedClass);
            result = factories.get(GET_KEY);
        }
        if (result == null) {
            throw new IllegalArgumentException("No transcoder registered for " + decodedClass + " and "
                    + transcodedClass);
        }
        return (ResourceTranscoder<Z, R>) result;
    }
}
