package com.bumptech.glide.load.resource;

import com.bumptech.glide.load.EncodeStrategy;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.engine.Resource;

import java.io.OutputStream;
import java.util.Map;

/**
 * A simple {@link com.bumptech.glide.load.ResourceEncoder} that never writes data.
 *
 * @param <T> The type of the resource that will always fail to be encoded.
 */
public class NullResourceEncoder<T> implements ResourceEncoder<T> {
    private static final NullResourceEncoder<?> NULL_ENCODER = new NullResourceEncoder<Object>();

    /**
     * Returns a NullResourceEncoder for the given type.
     *
     * @param <T> The type of data to be written (or in this case not written).
     */
    @SuppressWarnings("unchecked")
    public static <T> NullResourceEncoder<T> get() {
        return (NullResourceEncoder<T>) NULL_ENCODER;
    }

    @Override
    public boolean encode(Resource<T> data, OutputStream os, Map<String, Object> options) {
        return false;
    }

    @Override
    public EncodeStrategy getEncodeStrategy(Map<String, Object> options) {
        return EncodeStrategy.TRANSFORMED;
    }
}
