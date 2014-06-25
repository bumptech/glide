package com.bumptech.glide.load.model;

import com.bumptech.glide.load.Encoder;

import java.io.OutputStream;

public class NullEncoder<T> implements Encoder<T> {
    private static final NullEncoder NULL_ENCODER = new NullEncoder();

    @SuppressWarnings("unchecked")
    public static <T> NullEncoder<T> get() {
        return NULL_ENCODER;
    }

    @Override
    public boolean encode(T data, OutputStream os) {
        return false;
    }

    @Override
    public String getId() {
        return "";
    }
}
