package com.bumptech.glide.load;

import com.bumptech.glide.load.engine.Resource;

import java.io.IOException;

/**
 * An interface for decoding resources.
 *
 * @param <T> The type the resource will be decoded from (File, InputStream etc).
 * @param <Z> The type of the decoded resource (Bitmap, Drawable etc).
 */
public interface ResourceDecoder<T, Z> {

    /**
     * Returns a decoded resource from the given data or null if no resource could be decoded.
     * <p>
     *     The {@code source} is managed by the caller, there's no need to close it.
     *     The returned {@link Resource} will be {@link Resource#recycle() released} when the engine sees fit.
     * </p>
     * <p>
     *     Note - The {@code width} and {@code height} arguments are hints only,
     *     there is no requirement that the decoded resource exactly match the given dimensions.
     *     A typical use case would be to use the target dimensions to determine
     *     how much to downsample Bitmaps by to avoid overly large allocations.
     * </p>
     *
     * @param source The data the resource should be decoded from.
     * @param width The ideal width of the decoded resource.
     * @param height The ideal height of the decoded resource.
     * @throws IOException
     */
    public Resource<Z> decode(T source, int width, int height) throws IOException;

    /**
     * Returns an ID identifying any transformation this decoder may apply to the given data that will be mixed in to
     * the cache key.
     *
     * <p>
     *     If the decoder does not transform the data in a way that significantly affects the cached
     *     result (ie performs no downsampling) an empty string is an appropriate id.
     * </p>
     */
    public String getId();
}
