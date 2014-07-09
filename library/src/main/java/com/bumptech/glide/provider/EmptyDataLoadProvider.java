package com.bumptech.glide.provider;

import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;

/**
 * A {@link com.bumptech.glide.provider.DataLoadProvider} that returns {@code null} for every class.
 */
public class EmptyDataLoadProvider implements DataLoadProvider {
    private static final EmptyDataLoadProvider EMPTY_DATA_LOAD_PROVIDER = new EmptyDataLoadProvider();

    @SuppressWarnings("unchecked")
    public static <T, Z> DataLoadProvider<T, Z> get() {
        return EMPTY_DATA_LOAD_PROVIDER;
    }

    @Override
    public ResourceDecoder getCacheDecoder() {
        return null;
    }

    @Override
    public ResourceDecoder getSourceDecoder() {
        return null;
    }

    @Override
    public Encoder getSourceEncoder() {
        return null;
    }

    @Override
    public ResourceEncoder getEncoder() {
        return null;
    }
}
