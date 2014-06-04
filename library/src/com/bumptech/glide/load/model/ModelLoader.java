package com.bumptech.glide.load.model;

import com.bumptech.glide.load.model.stream.StreamStringLoader;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.resource.bitmap.BitmapDecoder;

/**
 * An interface for translating an arbitrarily complex data model into a concrete data type that can be used by an
 * {@link DataFetcher} to obtain the data for an image represented by the model.
 *
 * This interface has two objectives:
 *   1. To translate a specific data model into something that can be used by a decoder to load a Bitmap.
 *
 *   2. To allow a data model to be combined with the dimensions of the view to fetch an image of a specific size.
 *
 *      This not only avoids having to duplicate dimensions in xml and in your code in order to determine the size of a
 *      view on devices with different densities, but also allows you to use layout weights or otherwise
 *      programatically set the dimensions of the view without forcing you to fetch a generic image size.
 *
 *      The smaller the image you fetch, the less bandwidth and battery life you use, and the lower your memory
 *      footprint per image.
 *
 *
 * @param <T> The type of the model
 * @param <Y> The type of the data that can be used by a {@link BitmapDecoder}
 *           to decode a Bitmap.
 */
public interface ModelLoader<T, Y> {

    /**
     * Obtain an {@link DataFetcher} that can load the data required to decode the
     * image represented by this model. The {@link DataFetcher} will not be used if
     * the image is already cached.
     *
     * <p>
     *     Note - If the {@link StreamStringLoader} in any way retains a reference a context, either directly or as an
     *     anonymous inner class, that context may be leaked. The leak will only be an issue if this load can run for a
     *     long time or indefinitely (because of a particularly slow or paused/failed download for example).
     * </p>
     *
     *
     * @param model The model representing the image
     * @param width The width of the view the image will be loaded into
     * @param height The height of the view the image will be loaded into
     * @return A {@link DataFetcher} that can obtain the data for the image if the
     *          image is not cached.
     */
    public DataFetcher<Y> getResourceFetcher(T model, int width, int height);

    /**
     * Get a unique id for a given model
     *
     * @param model The model
     * @return A String that consistently and uniquely identifies this model. It can include width and height, but it
     *          does not have to.
     */
    public String getId(T model);
}
