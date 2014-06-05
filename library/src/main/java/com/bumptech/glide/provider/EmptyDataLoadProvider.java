package com.bumptech.glide.provider;

import com.bumptech.glide.DataLoadProvider;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;

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
    public ResourceEncoder getEncoder() {
        return null;
    }
}
