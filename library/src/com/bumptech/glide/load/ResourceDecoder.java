package com.bumptech.glide.load;

import com.bumptech.glide.Resource;

import java.io.IOException;

/**
 * An interface for decoding resources
 * @param <T> The type the resource will be decoded from (File, InputStream etc).
 * @param <Z> The type of the decoded resource (Bitmap, Drawable etc:w
 */
public interface ResourceDecoder<T, Z> {

    public Resource<Z> decode(T source, int width, int height) throws IOException;

    public String getId();
}
