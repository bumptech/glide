package com.bumptech.glide.load.data;

import com.bumptech.glide.Priority;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * A simple resource fetcher to convert byte arrays into input stream. Requires an id to be passed in to identify the
 * data in the byte array because there is no cheap/simple way to obtain a useful id from the data itself.
 */
public class ByteArrayFetcher implements DataFetcher<InputStream> {
    private final byte[] bytes;

    public ByteArrayFetcher(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public InputStream loadData(Priority priority) throws Exception {
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public void cleanup() {
        // Do nothing.
    }

    @Override
    public void cancel() {
        // Do nothing.
    }
}
