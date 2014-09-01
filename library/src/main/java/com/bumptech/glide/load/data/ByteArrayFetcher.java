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
    private final String id;

    public ByteArrayFetcher(byte[] bytes, String id) {
        this.bytes = bytes;
        this.id = id;
    }

    @Override
    public InputStream loadData(Priority priority) {
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public void cleanup() {
        // Do nothing. It's safe to leave a ByteArrayInputStream open.
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void cancel() {
        // Do nothing.
    }
}
