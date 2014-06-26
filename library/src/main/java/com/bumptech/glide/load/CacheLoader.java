package com.bumptech.glide.load;

import android.util.Log;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.cache.DiskCache;

import java.io.IOException;
import java.io.InputStream;

public class CacheLoader {
    private static final String TAG = "CacheLoader";
    private DiskCache diskCache;

    public CacheLoader(DiskCache diskCache) {
        this.diskCache = diskCache;
    }

    public <Z> Resource<Z> load(Key key, ResourceDecoder<InputStream, Z> decoder, int width, int height) {
        Resource<Z> result = null;
        InputStream fromCache = diskCache.get(key);
        if (fromCache != null) {
            try {
                result = decoder.decode(fromCache, width, height);
            } catch (IOException e) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Exception decoding image from cache", e);
                }
            }
            if (result == null) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Failed to decode image from cache or not present in cache");
                }
                diskCache.delete(key);
            }
        }
        return result;
    }
}
