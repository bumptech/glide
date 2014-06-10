package com.bumptech.glide.load;

import com.bumptech.glide.Resource;

import java.io.OutputStream;

public class SkipCache<T> implements ResourceEncoder<T> {
    private static final SkipCache SKIP_CACHE = new SkipCache();

    @SuppressWarnings("unchecked")
    public static <T> SkipCache<T> get() {
        return SKIP_CACHE;
    }

    @Override
    public boolean encode(Resource resource, OutputStream os) {
        return false;
    }

    @Override
    public String getId() {
        return "SkipCache.com.bumptech.glide.load";
    }
}
