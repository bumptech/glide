package com.bumptech.glide.resize.cache;

import com.bumptech.glide.resize.Key;

import java.io.InputStream;

public class DiskCacheAdapter implements DiskCache {
    @Override
    public InputStream get(Key key) {
        return null;
    }

    @Override
    public void put(Key key, Writer writer) { }

    @Override
    public void delete(Key key) { }
}
