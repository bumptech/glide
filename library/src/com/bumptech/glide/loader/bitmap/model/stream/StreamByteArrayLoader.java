package com.bumptech.glide.loader.bitmap.model.stream;

import com.bumptech.glide.loader.bitmap.resource.ByteArrayFetcher;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;

import java.io.InputStream;

/**
 * A base class to convert byte arrays to input streams so they can be decoded. This class is abstract because there is
 * no simple/quick way to generate an id from the bytes themselves, so subclass must include an id.
 */
public abstract class StreamByteArrayLoader implements StreamModelLoader<byte[]> {

    @Override
    public ResourceFetcher<InputStream> getResourceFetcher(byte[] model, int width, int height) {
        return new ByteArrayFetcher(model, getId(model));
    }
}
