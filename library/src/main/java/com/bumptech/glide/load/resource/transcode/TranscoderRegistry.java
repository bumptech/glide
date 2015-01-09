package com.bumptech.glide.load.resource.transcode;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that allows {@link com.bumptech.glide.load.resource.transcode.ResourceTranscoder}s to be registered and
 * retrieved by the classes they convert between.
 */
public class TranscoderRegistry {
    private final List<Entry<?, ?>> transcoders = new ArrayList<Entry<?, ?>>();

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
    public synchronized <Z, R> void register(Class<Z> decodedClass, Class<R> transcodedClass,
            ResourceTranscoder<Z, R> transcoder) {
        transcoders.add(new Entry<Z, R>(decodedClass, transcodedClass, transcoder));
    }

    /**
     * Returns the currently registered {@link com.bumptech.glide.load.resource.transcode.ResourceTranscoder} for the
     * given classes.
     *
     * @param resourceClass The class of the resource that the transcoder transcodes from.
     * @param transcodedClass The class of the resource that the transcoder transcodes to.
     * @param <Z> The type of the resource that the transcoder transcodes from.
     * @param <R> The type of the resource that the transcoder transcodes to.
     */
    @SuppressWarnings("unchecked")
    public synchronized <Z, R> ResourceTranscoder<Z, R> get(Class<Z> resourceClass, Class<R> transcodedClass) {
        if (resourceClass.equals(transcodedClass)) {
            return (ResourceTranscoder<Z, R>) UnitTranscoder.get();
        }
        for (Entry<?, ?> entry : transcoders) {
            if (entry.handles(resourceClass, transcodedClass)) {
                return (ResourceTranscoder<Z, R>) entry.transcoder;
            }
        }
        throw new IllegalArgumentException("No transcoder registered to transcode from " + resourceClass + " to "
                + transcodedClass);
    }

    private static final class Entry<Z, R> {
        private final Class<Z> fromClass;
        private final Class<R> toClass;
        private final ResourceTranscoder<Z, R> transcoder;

        private Entry(Class<Z> fromClass, Class<R> toClass, ResourceTranscoder<Z, R> transcoder) {
            this.fromClass = fromClass;
            this.toClass = toClass;
            this.transcoder = transcoder;
        }

        public boolean handles(Class<?> fromClass, Class<?> toClass) {
            // If we convert from a specific Drawable, we must get that specific Drawable class or a subclass of that
            // Drawable. In contrast, if we we convert <em>to</em> a specific Drawable, we can fulfill requests for
            // a more generic parent class (like Drawable). As a result, we check fromClass and toClass in different
            // orders.
            return this.fromClass.isAssignableFrom(fromClass) && toClass.isAssignableFrom(this.toClass);
        }
    }
}
