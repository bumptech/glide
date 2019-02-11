package com.bumptech.glide.load.engine.cache;

import android.content.Context;
import java.io.File;

/**
 * Creates an {@link com.bumptech.glide.disklrucache.DiskLruCache} based disk cache in the internal
 * disk cache directory.
 */
// Public API.
@SuppressWarnings({"WeakerAccess", "unused"})
public class InternalCacheDirectoryGetter implements CacheDirectoryGetter {

    private final Context context;
    private final String diskCacheName;

    InternalCacheDirectoryGetter(Context context, String diskCacheName) {
        this.context = context;
        this.diskCacheName = diskCacheName;
    }

    @Override
    public File getCacheDirectory() {
        File cacheDirectory = context.getCacheDir();
        if (cacheDirectory == null) {
            return null;
        }
        if (diskCacheName != null) {
            return new File(cacheDirectory, diskCacheName);
        }
        return cacheDirectory;
    }
}
