/*
 * Copyright (c) 2013. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.resize.cache;

import android.graphics.Bitmap;
import com.jakewharton.disklrucache.DiskLruCache;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 7/5/13
 * Time: 10:57 AM
 * To change this template use File | Settings | File Templates.
 */
public class DiskLruCacheWrapper implements DiskCache {

    private static DiskLruCache CACHE = null;

    private synchronized static DiskLruCache getDiskLruCache(File directory, int maxSize) throws IOException {
        if (CACHE == null) {
            CACHE = DiskLruCache.open(directory, 0, 1, maxSize);
        }
        return CACHE;
    }

    public static DiskLruCacheWrapper get(File directory, int maxSize) throws IOException {
        return new DiskLruCacheWrapper(getDiskLruCache(directory, maxSize));
    }

    private final DiskLruCache diskLruCache;

    protected DiskLruCacheWrapper(DiskLruCache diskLruCache) {
        this.diskLruCache = diskLruCache;
    }

    @Override
    public Bitmap get(String key, Reader reader) {
        Bitmap result = null;
        try {
            //It is possible that the there will be a put in between these two gets. If so that shouldn't be a problem
            //because we will always put the same value at the same key so our input streams will still represent
            //the same data
            final DiskLruCache.Snapshot snapshot1 = diskLruCache.get(key);
            if (snapshot1 != null) {
                final DiskLruCache.Snapshot snapshot2 = diskLruCache.get(key);
                if (snapshot2 != null) {
                    final InputStream is1 = snapshot1.getInputStream(0);
                    final InputStream is2 = snapshot2.getInputStream(0);
                    result = reader.read(is1, is2);
                }
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
                writer.write(editor.newOutputStream(0));
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
