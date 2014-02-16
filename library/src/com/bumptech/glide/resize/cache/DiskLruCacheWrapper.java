/*
 * Copyright (c) 2013. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.glide.resize.cache;

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

    private static final int APP_VERSION = 1;
    private static final int VALUE_COUNT = 1;
    private static DiskLruCache CACHE = null;
    private static DiskLruCacheWrapper WRAPPER = null;

    private synchronized static DiskLruCache getDiskLruCache(File directory, int maxSize) throws IOException {
        if (CACHE == null) {
            CACHE = DiskLruCache.open(directory, APP_VERSION, VALUE_COUNT, maxSize);
        }
        return CACHE;
    }

    /**
     * Get a DiskCache in the given directory and size. If a disk cache has alread been created with
     * a different directory and/or size, it will be returned instead and the new arguments
     * will be ignored.
     *
     * @param directory The directory for the disk cache
     * @param maxSize The max size for the disk cache
     * @return The new disk cache with the given arguments, or the current cache if one already exists
     * @throws IOException
     */
    public synchronized static DiskCache get(File directory, int maxSize) throws IOException {
        if (WRAPPER == null) {
            WRAPPER = new DiskLruCacheWrapper(getDiskLruCache(directory, maxSize));
        }
        return WRAPPER;
    }

    private final DiskLruCache diskLruCache;

    protected DiskLruCacheWrapper(DiskLruCache diskLruCache) {
        this.diskLruCache = diskLruCache;
    }

    @Override
    public InputStream get(String key) {
        InputStream result = null;
        try {
            //It is possible that the there will be a put in between these two gets. If so that shouldn't be a problem
            //because we will always put the same value at the same key so our input streams will still represent
            //the same data
            final DiskLruCache.Snapshot snapshot = diskLruCache.get(key);
            if (snapshot != null) {
                result = snapshot.getInputStream(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public void put(String key, Writer writer) {
        try {
            DiskLruCache.Editor editor = diskLruCache.edit(key);
            //editor will be null if there are two concurrent puts
            //worst case just silently fail
            if (editor != null) {
                OutputStream os = null;
                try {
                    os = editor.newOutputStream(0);
                    writer.write(os);
                } finally {
                    if (os != null) {
                        os.close();
                    }
                }
                editor.commit();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(String key) {
        try {
            diskLruCache.remove(key);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
