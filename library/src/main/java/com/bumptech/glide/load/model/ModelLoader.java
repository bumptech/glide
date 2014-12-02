package com.bumptech.glide.load.model;

import com.bumptech.glide.load.data.DataFetcher;

/**
 * A factory interface for translating an arbitrarily complex data model into a concrete data type that can be used
 * by an {@link DataFetcher} to obtain the data for a resource represented by the model.
 *
 * <p>
 *  This interface has two objectives:
 *   1. To translate a specific model into a data type that can be decoded into a resource.
 *
 *   2. To allow a model to be combined with the dimensions of the view to fetch a resource of a specific size.
 *
 *      This not only avoids having to duplicate dimensions in xml and in your code in order to determine the size of a
 *      view on devices with different densities, but also allows you to use layout weights or otherwise
 *      programatically set the dimensions of the view without forcing you to fetch a generic resource size.
 *
 *      The smaller the resource you fetch, the less bandwidth and battery life you use, and the lower your memory
 *      footprint per resource.
 *</p>
 *
 * @param <T> The type of the model.
 * @param <Y> The type of the data that can be used by a {@link com.bumptech.glide.load.ResourceDecoder} to decode a
 *           resource.
 */
public interface ModelLoader<T, Y> {

    /**
     * Obtains an {@link DataFetcher} that can fetch the data required to decode the resource represented by this model.
     * The {@link DataFetcher} will not be used if the resource is already cached.
     *
     * <p>
     *     Note - If no valid data fetcher can be returned (for example if a model has a null URL), then it is
     *     acceptable to return a null data fetcher from this method. Doing so will be treated any other failure or
     *     exception during the load process.
     * </p>
     *
     * @param model The model representing the resource.
     * @param width The width in pixels of the view or target the resource will be loaded into, or
     *              {@link com.bumptech.glide.request.target.Target#SIZE_ORIGINAL} to indicate that the resource should
     *              be loaded at its original width.
     * @param height The height in pixels of the view or target the resource will be loaded into, or
     *               {@link com.bumptech.glide.request.target.Target#SIZE_ORIGINAL} to indicate that the resource should
     *               be loaded at its original height.
     * @return A {@link DataFetcher} that can obtain the data the resource can be decoded from if the resource is not
     * cached, or null if no valid {@link com.bumptech.glide.load.data.DataFetcher} could be constructed.
     */
    DataFetcher<Y> getResourceFetcher(T model, int width, int height);
}
