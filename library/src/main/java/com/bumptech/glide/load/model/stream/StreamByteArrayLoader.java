package com.bumptech.glide.load.model.stream;

import com.bumptech.glide.load.data.ByteArrayFetcher;
import com.bumptech.glide.load.data.DataFetcher;

import java.io.InputStream;
import java.util.UUID;

/**
 * A base class to convert byte arrays to input streams so they can be decoded. This class is abstract because there is
 * no simple/quick way to generate an id from the bytes themselves, so subclass must include an id.
 */
public class StreamByteArrayLoader implements StreamModelLoader<byte[]> {

    @Override
    public DataFetcher<InputStream> getResourceFetcher(byte[] model, int width, int height) {
        return new ByteArrayFetcher(model);
    }

    @Override
    public String getId(byte[] model) {
        return UUID.randomUUID().toString();
    }
}
