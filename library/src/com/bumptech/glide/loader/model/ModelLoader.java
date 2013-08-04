package com.bumptech.glide.loader.model;

import com.bumptech.glide.loader.stream.StreamLoader;

/**
 * An interface for translating an arbitrarily complex data model into a concrete data type that can be
 * used by an {@link StreamLoader} to obtain an {@link java.io.InputStream} for an image represented by the model.
 *
 * This interface has two objectives:
 *   1. To translate a specific data model into something that can be used generically to open an
 *      {@link java.io.InputStream}
 *   2. To allow a data model to be combined with the dimensions of the view to fetch an image of a specific size.
 *
 *      This not only avoids having to duplicate dimensions in xml and in your code in order to determine the size of a
 *      view on devices with different densities, but also allows you to use layout weights or otherwise
 *      programatically set the dimensions of the view without forcing you to fetch a generic image size
 *
 *      The smaller the image you fetch, the less bandwidth and battery life you use, and the lower your memory
 *      footprint per image.
 *
 *
 * @param <T> The type of the data model
 */
public interface ModelLoader<T> {

    /**
     * Obtain an {@link StreamLoader} that can asynchronously load and open an InputStream for the image represented
     * by this model. The {@link StreamLoader} will not be used if the image is already cached.
     *
     * @param model The model representing the image
     * @param width The width of the view the image will be loaded into
     * @param height The height of the view the image will be loaded into
     * @return A {@link StreamLoader} that can obtain an InputStream for the image if it is not cached
     */
    public StreamLoader getStreamLoader(T model, int width, int height);


    /**
     * Get a unique id for a given model
     *
     * @param model The model
     * @return A String that consistently and uniquely identifies this model. It can include width and height, but
     * it does not have to.
     */
    public String getId(T model);
}
