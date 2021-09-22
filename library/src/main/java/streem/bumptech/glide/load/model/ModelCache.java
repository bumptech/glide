package com.bumptech.glide.load.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.bumptech.glide.util.LruCache;
import com.bumptech.glide.util.Util;
import java.util.Queue;

/**
 * A simple cache that can be used by {@link ModelLoader} and {@link ModelLoaderFactory} to cache
 * some data for a given model, width and height. For a loader that takes a model and returns a url,
 * the cache could be used to safely memoize url creation based on the width and height of the view.
 *
 * @param <A> Some Model type that implements {@link #equals} and {@link #hashCode}.
 * @param <B> Some useful type that may be expensive to create (URL, file path, etc).
 */
public class ModelCache<A, B> {
  private static final int DEFAULT_SIZE = 250;

  private final LruCache<ModelKey<A>, B> cache;

  // Public API.
  @SuppressWarnings("unused")
  public ModelCache() {
    this(DEFAULT_SIZE);
  }

  public ModelCache(long size) {
    cache =
        new LruCache<ModelKey<A>, B>(size) {
          @Override
          protected void onItemEvicted(@NonNull ModelKey<A> key, @Nullable B item) {
            key.release();
          }
        };
  }

  /**
   * Get a value.
   *
   * @param model The model.
   * @param width The width in pixels of the view the image is being loaded into.
   * @param height The height in pixels of the view the image is being loaded into.
   * @return The cached result, or null.
   */
  @Nullable
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
   * @param width The width in pixels of the view the image is being loaded into.
   * @param height The height in pixels of the view the image is being loaded into.
   * @param value The value to store.
   */
  public void put(A model, int width, int height, B value) {
    ModelKey<A> key = ModelKey.get(model, width, height);
    cache.put(key, value);
  }

  /** Removes all entries from the cache. */
  public void clear() {
    cache.clearMemory();
  }

  @VisibleForTesting
  static final class ModelKey<A> {
    private static final Queue<ModelKey<?>> KEY_QUEUE = Util.createQueue(0);

    private int height;
    private int width;
    private A model;

    @SuppressWarnings("unchecked")
    static <A> ModelKey<A> get(A model, int width, int height) {
      ModelKey<A> modelKey;
      synchronized (KEY_QUEUE) {
        modelKey = (ModelKey<A>) KEY_QUEUE.poll();
      }
      if (modelKey == null) {
        modelKey = new ModelKey<>();
      }

      modelKey.init(model, width, height);
      return modelKey;
    }

    private ModelKey() {}

    private void init(A model, int width, int height) {
      this.model = model;
      this.width = width;
      this.height = height;
    }

    public void release() {
      synchronized (KEY_QUEUE) {
        KEY_QUEUE.offer(this);
      }
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof ModelKey) {
        @SuppressWarnings("unchecked")
        ModelKey<A> other = (ModelKey<A>) o;
        return width == other.width && height == other.height && model.equals(other.model);
      }
      return false;
    }

    @Override
    public int hashCode() {
      int result = height;
      result = 31 * result + width;
      result = 31 * result + model.hashCode();
      return result;
    }
  }
}
