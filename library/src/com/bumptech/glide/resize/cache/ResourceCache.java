package com.bumptech.glide.resize.cache;

import com.bumptech.glide.resize.Resource;
import com.bumptech.glide.util.LruCache;

public class ResourceCache extends LruCache<String, Resource> {
    private ResourceRemovedListener listener;

    public interface ResourceRemovedListener {
        public void onResourceRemoved(Resource resource);
    }

    public ResourceCache(int size) {
        super(size);
    }

    public void setResourceRemovedListener(ResourceRemovedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onItemRemoved(Resource item) {
        if (listener != null) {
            listener.onResourceRemoved(item);
        }
    }

    @Override
    protected int getSize(Resource item) {
        return item.getSize();
    }
}
