package com.bumptech.glide.load.model;

import com.bumptech.glide.util.LruCache;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * A simple cache that can be used by {@link ModelLoader} and {@link ModelLoaderFactory} to cache some data for a given
 * model, width and height. For a loader that takes a model and returns a url, the cache could be used to safely memoize
 * url creation based on the width and height of the view.
 *
 * @param <A> Some Model type that implements equals and hashcode.
 * @param <B> Some useful type that may be expensive to create (URL, file path, etc).
 * //TODO: fix this.
 */
public class ModelCache<A, B> {
    private static final int DEFAULT_SIZE = 250;

    private static class ModelKey<A> {
        private static final Queue<ModelKey> KEY_QUEUE = new ArrayDeque<ModelKey>();

        @SuppressWarnings("unchecked")
        public static <A> ModelKey<A> get(A model, int width, int height) {
            ModelKey<A> modelKey = KEY_QUEUE.poll();
            if (modelKey == null) {
                modelKey = new ModelKey<A>();
            }

            modelKey.init(model, width, height);
            return modelKey;
        }

        private int height;
        private int width;
        private A model;

        private ModelKey() {  }

        private void init(A model, int width, int height) {
            this.model = model;
            this.width = width;
            this.height = height;
        }

        public void release() {
            KEY_QUEUE.offer(this);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ModelKey modelKey = (ModelKey) o;

            if (height != modelKey.height) return false;
            if (width != modelKey.width) return false;
            if (!model.equals(modelKey.model)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = height;
            result = 31 * result + width;
            result = 31 * result + model.hashCode();
            return result;
        }
    }

    private final LruCache<ModelKey<A>, B> cache;

    public ModelCache() {
        this(DEFAULT_SIZE);
    }

    public ModelCache(int size) {
        cache = new LruCache<ModelKey<A>, B>(size) {
            @Override
            protected void onItemRemoved(ModelKey<A> key, B item) {
                key.release();
            }
        };
    }

    /**
     * Get a value.
     *
     * @param model The model.
     * @param width The width of the view the image is being loaded into.
     * @param height The height of the view the image is being loaded into.
     *
     * @return The cached result, or null.
     */
    public B get(A model, int width, int height) {
        ModelKey<A> key = ModelKey.get(model, width, height);
        B result = cache.get(key);
        key.release();
        return result;
    }

    /**
     * Add a value.
     *
     * @param model The model.
     * @param width The width of the view the image is being loaded into.
     * @param height The height of the view the image is being loaded into.
     * @param value The value to store.
     */
    public void put(A model, int width, int height, B value) {
        ModelKey<A> key = ModelKey.get(model, width, height);
        cache.put(key, value);
    }
}
