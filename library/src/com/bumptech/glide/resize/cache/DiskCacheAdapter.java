package com.bumptech.glide.resize.cache;

import java.io.InputStream;

public class DiskCacheAdapter implements DiskCache {
    @Override
    public InputStream get(String key) {
        return null;
    }

    @Override
    public void put(String key, Writer writer) { }

    @Override
    public void delete(String key) { }
}
