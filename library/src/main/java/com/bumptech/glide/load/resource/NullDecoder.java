package com.bumptech.glide.load.resource;

import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;

/**
 * A simple {@link com.bumptech.glide.load.ResourceDecoder} that always returns null.
 *
 * @param <T> The type of the data that will be ignored by this class.
 * @param <Z> The type of the decoded resource that will always be null.
 */
public class NullDecoder<T, Z> implements ResourceDecoder<T, Z> {
    private static final NullDecoder<?, ?> NULL_DECODER = new NullDecoder<Object, Object>();

    /**
     * Returns an instance of the NullDecoder for the given types.
     *
     * @param <T> The data type.
     * @param <Z> The resource type.
     */
    @SuppressWarnings("unchecked")
    public static <T, Z> NullDecoder<T, Z> get() {
        return (NullDecoder<T, Z>) NULL_DECODER;
    }

    @Override
    public Resource<Z> decode(T source, int width, int height) {
        return null;
    }

    @Override
    public String getId() {
        return "";
    }
}
