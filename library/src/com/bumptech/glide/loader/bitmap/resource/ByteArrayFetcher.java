package com.bumptech.glide.loader.bitmap.resource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * A simple resource fetcher to convert byte arrays into input stream. Requires an id to be passed in to identify the
 * data in the byte array because there is no cheap/simple way to obtain a useful id from the data itself.
 */
public class ByteArrayFetcher implements ResourceFetcher<InputStream> {
    private final byte[] bytes;
    private final String id;

    public ByteArrayFetcher(byte[] bytes, String id) {
        this.bytes = bytes;
        this.id = id;
    }

    @Override
    public InputStream loadResource() throws Exception {
        return new ByteArrayInputStream(bytes);
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
