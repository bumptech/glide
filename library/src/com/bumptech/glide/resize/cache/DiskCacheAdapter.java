package com.bumptech.glide.resize.cache;

import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 6/5/13
 * Time: 10:24 AM
 * To change this template use File | Settings | File Templates.
 */
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
