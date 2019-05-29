package com.bumptech.glide.load.engine.cache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.Resource;

/** An interface for adding and removing resources from an in memory cache. */
public interface MemoryCache {
  /** An interface that will be called whenever a bitmap is removed from the cache. */
  interface ResourceRemovedListener {
    void onResourceRemoved(@NonNull Resource<?> removed);
  }

  /** Returns the sum of the sizes of all the contents of the cache in bytes. */
  long getCurrentSize();

  /** Returns the current maximum size in bytes of the cache. */
  long getMaxSize();

  /**
   * Adjust the maximum size of the cache by multiplying the original size of the cache by the given
   * multiplier.
   *
   * <p>If the size multiplier causes the size of the cache to be decreased, items will be evicted
   * until the cache is smaller than the new size.
   *
   * @param multiplier A size multiplier >= 0.
   */
  void setSizeMultiplier(float multiplier);

  /**
   * Removes the value for the given key and returns it if present or null otherwise.
   *
   * @param key The key.
   */
  @Nullable
  Resource<?> remove(@NonNull Key key);

  /**
   * Add bitmap to the cache with the given key.
   *
   * @param key The key to retrieve the bitmap.
   * @param resource The {@link com.bumptech.glide.load.engine.EngineResource} to store.
   * @return The old value of key (null if key is not in map).
   */
  @Nullable
  Resource<?> put(@NonNull Key key, @Nullable Resource<?> resource);

  /**
   * Set the listener to be called when a bitmap is removed from the cache.
   *
   * @param listener The listener.
   */
  void setResourceRemovedListener(@NonNull ResourceRemovedListener listener);

  /** Evict all items from the memory cache. */
  void clearMemory();

  /**
   * Trim the memory cache to the appropriate level. Typically called on the callback onTrimMemory.
   *
   * @param level This integer represents a trim level as specified in {@link
   *     android.content.ComponentCallbacks2}.
   */
  void trimMemory(int level);
}
