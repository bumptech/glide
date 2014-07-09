package com.bumptech.glide.load.resource.transcode;

import java.util.HashMap;
import java.util.Map;

/**
 * A class that allows {@link com.bumptech.glide.load.resource.transcode.ResourceTranscoder}s to be registered and
 * retrieved by the classes they convert between.
 */
public class TranscoderRegistry {
    private static final MultiClassKey GET_KEY = new MultiClassKey();

    private final Map<MultiClassKey, ResourceTranscoder> factories = new HashMap<MultiClassKey, ResourceTranscoder>();

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
            return UnitTranscoder.get();
        }
        GET_KEY.set(decodedClass, transcodedClass);
        ResourceTranscoder<Z, R> result = factories.get(GET_KEY);
        if (result == null) {
            throw new IllegalArgumentException("No transcoder registered for " + decodedClass + " and "
                    + transcodedClass);
        }
        return result;
    }

    private static class MultiClassKey {
        private Class decoded;
        private Class transcoded;

        public MultiClassKey() {
            // Empty.
        }

        public MultiClassKey(Class decoded, Class transcoded) {
            this.decoded = decoded;
            this.transcoded = transcoded;
        }

        public void set(Class decoded, Class transcoded) {
            this.decoded = decoded;
            this.transcoded = transcoded;
        }

        @Override
        public String toString() {
            return "MultiClassKey{"
                    + "decoded=" + decoded
                    + ", transcoded=" + transcoded
                    + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            MultiClassKey that = (MultiClassKey) o;

            if (!decoded.equals(that.decoded)) {
                return false;
            }
            if (!transcoded.equals(that.transcoded)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = decoded.hashCode();
            result = 31 * result + transcoded.hashCode();
            return result;
        }
    }
}
