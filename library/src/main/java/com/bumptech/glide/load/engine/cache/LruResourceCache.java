package com.bumptech.glide.load.engine.cache;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.BitmapInfo;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.util.LruCache;
import java.util.ArrayList;
import java.util.List;

/** An LRU in memory cache for {@link com.bumptech.glide.load.engine.Resource}s. */
public class LruResourceCache extends LruCache<Key, Resource<?>> implements MemoryCache {
  private ResourceRemovedListener listener;

  /**
   * Constructor for LruResourceCache.
   *
   * @param size The maximum size in bytes the in memory cache can use.
   */
  public LruResourceCache(long size) {
    super(size);
  }

  @Override
  public void setResourceRemovedListener(@NonNull ResourceRemovedListener listener) {
    this.listener = listener;
  }

  @Override
  protected void onItemEvicted(@NonNull Key key, @Nullable Resource<?> item) {
    if (listener != null && item != null) {
      listener.onResourceRemoved(item);
    }
  }

  @Override
  protected int getSize(@Nullable Resource<?> item) {
    if (item == null) {
      return super.getSize(null);
    } else {
      return item.getSize();
    }
  }

  @SuppressLint("InlinedApi")
  @Override
  public void trimMemory(int level) {
    if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
      // Entering list of cached background apps
      // Evict our entire bitmap cache
      clearMemory();
    } else if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
        || level == android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
      // The app's UI is no longer visible, or app is in the foreground but system is running
      // critically low on memory
      // Evict oldest half of our bitmap cache
      trimToSize(getMaxSize() / 2);
    }
  }

  @NonNull
  @Override
  public synchronized List<MemoryCacheEntryInfo> getCacheEntryInfos() {
    List<MemoryCacheEntryInfo> result = new ArrayList<>();
    for (LruCache.EntryInfo<Resource<?>> entry : super.getSnapshot()) {
      if (entry.value != null) {
        Bitmap bitmap = getBitmap(entry.value);
        if (bitmap != null) {
          result.add(new MemoryCacheEntryInfo(new BitmapInfo(bitmap), entry.lastAccessedTimestamp));
        }
      }
    }
    return result;
  }

  @Nullable
  private static Bitmap getBitmap(Resource<?> resource) {
    Object value = resource.get();
    if (value instanceof Bitmap) {
      return (Bitmap) value;
    } else if (value instanceof BitmapDrawable) {
      return ((BitmapDrawable) value).getBitmap();
    }
    return null;
  }
}
