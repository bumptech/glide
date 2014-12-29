package com.bumptech.glide.load.engine.cache;

import android.content.Context;

import com.bumptech.glide.Glide;

import java.io.File;

/**
 * Creates an {@link com.bumptech.glide.disklrucache.DiskLruCache} based disk cache in the internal disk cache
 * directory.
 */
public final class InternalCacheDiskCacheFactory implements DiskCache.Factory {
    private final Context context;
    private final String diskCacheName;
    private final int diskCacheSize;

    public InternalCacheDiskCacheFactory(Context context, int diskCacheSize) {
        this(context, null /*diskCacheName*/, diskCacheSize);
    }

    public InternalCacheDiskCacheFactory(Context context, String diskCacheName, int diskCacheSize) {
        this.context = context;
        this.diskCacheName = diskCacheName;
        this.diskCacheSize = diskCacheSize;
    }

    @Override
    public DiskCache build() {
        DiskCache diskCache = null;
        final File cacheDir;

        if (diskCacheName != null) {
            cacheDir = Glide.getPhotoCacheDir(context, diskCacheName);
        } else {
            cacheDir = Glide.getPhotoCacheDir(context);
        }

        if (cacheDir != null) {
            diskCache = DiskLruCacheWrapper.get(cacheDir, diskCacheSize);
        }

        if (diskCache == null) {
            diskCache = new DiskCacheAdapter();
        }
        return diskCache;
    }
}
