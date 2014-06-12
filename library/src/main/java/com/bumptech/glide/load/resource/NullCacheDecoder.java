package com.bumptech.glide.load.resource;

import com.bumptech.glide.Resource;
import com.bumptech.glide.load.ResourceDecoder;

import java.io.IOException;
import java.io.InputStream;

public class NullCacheDecoder<Z> implements ResourceDecoder<InputStream, Z> {
    private static final NullCacheDecoder NULL_DECODER = new NullCacheDecoder();

    @SuppressWarnings("unchecked")
    public static <Z> NullCacheDecoder<Z> get() {
        return NULL_DECODER;
    }

    @Override
    public Resource<Z> decode(InputStream source, int width, int height) throws IOException {
        return null;
    }

    @Override
    public String getId() {
        return "";
    }
}
