package com.bumptech.glide.resize.cache;

import com.bumptech.glide.resize.Resource;
import com.bumptech.glide.util.LruCache;

public class ResourceCache extends LruCache<String, Resource> {
    public ResourceCache(int size) {
        super(size);
    }

    @Override
    protected int getSize(Resource item) {
        return item.getSize();
    }
}
