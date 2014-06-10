/*
 * Copyright (c) 2013. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.glide.load.engine.cache;

import android.util.Log;
import com.bumptech.glide.load.Key;
import com.jakewharton.disklrucache.DiskLruCache;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The default DiskCache implementation. There must be no more than one active instance for a given
 * directory at a time.
 *
 * @see #get(java.io.File, int)
 */
public class DiskLruCacheWrapper implements DiskCache {
    private static final String TAG = "DiskLruCacheWrapper";

    private static final int APP_VERSION = 1;
    private static final int VALUE_COUNT = 1;
    private static DiskLruCacheWrapper WRAPPER = null;
    private final SafeKeyGenerator safeKeyGenerator;

    /**
     * Get a DiskCache in the given directory and size. If a disk cache has alread been created with
     * a different directory and/or size, it will be returned instead and the new arguments
     * will be ignored.
     *
     * @param directory The directory for the disk cache
     * @param maxSize The max size for the disk cache
     * @return The new disk cache with the given arguments, or the current cache if one already exists
     */
    public synchronized static DiskCache get(File directory, int maxSize) {
        if (WRAPPER == null) {
            WRAPPER = new DiskLruCacheWrapper(directory, maxSize);
        }
        return WRAPPER;
    }

    private final File directory;
    private final int maxSize;

    private DiskLruCache diskLruCache;

    protected DiskLruCacheWrapper(File directory, int maxSize) {
        this.directory = directory;
        this.maxSize = maxSize;
        this.safeKeyGenerator = new SafeKeyGenerator();
    }

    private synchronized DiskLruCache getDiskCache() throws IOException {
        if (diskLruCache == null) {
            diskLruCache = DiskLruCache.open(directory, APP_VERSION, VALUE_COUNT, maxSize);
        }
        return diskLruCache;
    }

    @Override
    public InputStream get(Key key) {
        String safeKey = safeKeyGenerator.getSafeKey(key);
        InputStream result = null;
        try {
            //It is possible that the there will be a put in between these two gets. If so that shouldn't be a problem
            //because we will always put the same value at the same key so our input streams will still represent
            //the same data
            final DiskLruCache.Snapshot snapshot = getDiskCache().get(safeKey);
            if (snapshot != null) {
                result = snapshot.getInputStream(0);
            }
        } catch (IOException e) {
            if (Log.isLoggable(TAG, Log.WARN)) {
                Log.w(TAG, "Unable to get from disk cache", e);
            }
        }
        return result;
    }

    @Override
    public void put(Key key, Writer writer) {
        String safeKey = safeKeyGenerator.getSafeKey(key);
        try {
            DiskLruCache.Editor editor = getDiskCache().edit(safeKey);
            //editor will be null if there are two concurrent puts
            //worst case just silently fail
            if (editor != null) {
                boolean success = false;
                OutputStream os = null;
                try {
                    os = editor.newOutputStream(0);
                    success = writer.write(os);
                } finally {
                    if (os != null) {
                        os.close();
                    }
                }
                if (success) {
                    editor.commit();
                }
            }
        } catch (IOException e) {
            if (Log.isLoggable(TAG, Log.WARN)) {
                Log.w(TAG, "Unable to put to disk cache", e);
            }
        }
    }

    @Override
    public void delete(Key key) {
        String safeKey = safeKeyGenerator.getSafeKey(key);
        try {
            getDiskCache().remove(safeKey);
        } catch (IOException e) {
            if (Log.isLoggable(TAG, Log.WARN)) {
                Log.w(TAG, "Unable to delete from disk cache", e);
            }
        }
    }
}
