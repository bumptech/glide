package com.bumptech.glide.load.resource;

import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;

import java.io.IOException;

public class NullDecoder<T, Z> implements ResourceDecoder<T, Z> {
    private static final NullDecoder NULL_DECODER = new NullDecoder();

    @SuppressWarnings("unchecked")
    public static <T, Z> NullDecoder<T, Z> get() {
        return NULL_DECODER;
    }

    @Override
    public Resource<Z> decode(T source, int width, int height) throws IOException {
        return null;
    }

    @Override
    public String getId() {
        return "";
    }
}
