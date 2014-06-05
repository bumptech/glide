package com.bumptech.glide.load.model;

import com.bumptech.glide.util.LruCache;

/**
 * A simple cache that can be used by {@link ModelLoader} and {@link ModelLoaderFactory} to cache some data for a given
 * model, width and height. For a loader that takes a model and returns a url, the cache could be used to safely memoize
 * url creation based on the width and height of the view.
 *
 * @param <A> Some useful type that may be expensive to create (URL, file path, etc).
 */
public class ModelCache<A> {
    private static final int DEFAULT_SIZE = 250;

    private static class ModelKey {
        private final int height;
        private final int width;
        private final String id;

        public ModelKey(String id, int width, int height) {
            this.id = id;
            this.width = width;
            this.height = height;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ModelKey modelKey = (ModelKey) o;

            if (height != modelKey.height) return false;
            if (width != modelKey.width) return false;
            if (!id.equals(modelKey.id)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = height;
            result = 31 * result + width;
            result = 31 * result + id.hashCode();
            return result;
        }
    }

    private final LruCache<ModelKey, A> cache;

    public ModelCache() {
        this(DEFAULT_SIZE);
    }

    public ModelCache(int size) {
        cache = new LruCache<ModelKey, A>(size);
    }

    /**
     * Get a value
     *
     * @param id The string id for the given model from {@link ModelLoader#getId(Object)}
     * @param width The width of the view the image is being loaded into
     * @param height The height of the view the image is being loaded into
     *
     * @return The cached result, or null
     */
    public A get(String id, int width, int height) {
        return cache.get(new ModelKey(id, width, height));
    }

    /**
     * Add a value
     *
     * @param id The string id for the given model from {@link ModelLoader#getId(Object)}
     * @param width The width of the view the image is being loaded into
     * @param height The height of the view the image is being loaded into
     * @param value The value to store
     */
    public void put(String id, int width, int height, A value) {
        cache.put(new ModelKey(id, width, height), value);
    }
}
