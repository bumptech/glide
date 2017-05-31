package com.bumptech.glide.load.engine.cache;

import android.annotation.SuppressLint;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.util.LruCache;

/**
 * An LRU in memory cache for {@link com.bumptech.glide.load.engine.Resource}s.
 */
public class LruResourceCache extends LruCache<Key, Resource<?>> implements MemoryCache {
  private ResourceRemovedListener listener;

  /**
   * Constructor for LruResourceCache.
   *
   * @param size The maximum size in bytes the in memory cache can use.
   */
  public LruResourceCache(int size) {
    super(size);
  }

  @Override
  public void setResourceRemovedListener(ResourceRemovedListener listener) {
    this.listener = listener;
  }

  @Override
  protected void onItemEvicted(Key key, Resource<?> item) {
    if (listener != null) {
      listener.onResourceRemoved(item);
    }
  }

  @Override
  protected int getSize(Resource<?> item) {
    return item.getSize();
  }

  @SuppressLint("InlinedApi")
  @Override
  public void trimMemory(int level) {
    if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
      // Nearing middle of list of cached background apps
      // Evict our entire bitmap cache
      clearMemory();
    } else if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
      // Entering list of cached background apps
      // Evict oldest half of our bitmap cache
      trimToSize(getCurrentSize() / 2);
    }
  }
}
