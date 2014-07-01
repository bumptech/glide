package com.bumptech.glide.load;

import com.bumptech.glide.load.engine.Resource;

import java.io.OutputStream;

public class NullResourceEncoder<T> implements ResourceEncoder<T> {
    private static final NullResourceEncoder SKIP_CACHE = new NullResourceEncoder();

    @SuppressWarnings("unchecked")
    public static <T> NullResourceEncoder<T> get() {
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
