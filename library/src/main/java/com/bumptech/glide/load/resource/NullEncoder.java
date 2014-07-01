package com.bumptech.glide.load.resource;

import com.bumptech.glide.load.Encoder;

import java.io.OutputStream;

public class NullEncoder implements Encoder {
    private static final NullEncoder NULL_ENCODER = new NullEncoder();

    @SuppressWarnings("unchecked")
    public static <T> Encoder<T> get() {
        return NULL_ENCODER;

    }

    @Override
    public boolean encode(Object data, OutputStream os) {
        return false;
    }

    @Override
    public String getId() {
        return "";
    }
}
