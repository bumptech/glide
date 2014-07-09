package com.bumptech.glide.load.engine.cache;

import com.bumptech.glide.load.Key;

import java.io.File;

/**
 * A simple class that returns null for all gets and ignores all writes.
 */
public class DiskCacheAdapter implements DiskCache {
    @Override
    public File get(Key key) {
        return null;
    }

    @Override
    public void put(Key key, Writer writer) { }

    @Override
    public void delete(Key key) { }
}
