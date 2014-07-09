package com.bumptech.glide.load.resource;

import com.bumptech.glide.load.Encoder;

import java.io.OutputStream;

/**
 * A simple {@link com.bumptech.glide.load.Encoder} that never writes data.
 */
public class NullEncoder implements Encoder {
    private static final NullEncoder NULL_ENCODER = new NullEncoder();

    /**
     * Returns an Encoder for the given data type.
     *
     * @param <T> The type of data to be written (or not in this case).
     */
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
