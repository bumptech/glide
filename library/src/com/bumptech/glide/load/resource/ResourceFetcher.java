package com.bumptech.glide.load.resource;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.model.ModelLoader;

/**
 * A base class for lazily and retrieving a resource that can be used to load an image.
 * A new instance is created per image load by {@link ModelLoader}. loadResource
 * may or may not be called for any given load depending on whether or not the corresponding image is cached. Cancel
 * also may or may not be called.
 *
 * @param <T> The type of resource to be loaded.
 */
public interface ResourceFetcher<T> {

    /**
     * Asynchronously fetch a resource representing an image. This will always be called on
     * background thread so it is safe to perform long running tasks here. Any third party libraries called
     * must be thread safe since this method will be called from a thread in a
     * {@link java.util.concurrent.ExecutorService} that may have more than one background thread.
     *
     * This method will only be called when the corresponding image is not in the cache.
     *
     * @param priority The priority with which the request should be completed.
     */
    public T loadResource(Priority priority) throws Exception;

    /**
     * Cleanup or recycle any resources used by this resource fetcher. This method will be called in a finally block
     * after the data returned by {@link #loadResource(Priority)} has been decoded by the {@link ResourceDecoder}.
     */
    public void cleanup();

    /**
     * A method that will be called when a load is no longer relevant and has been cancelled. This method does not need
     * to guarantee that any in process loads do not finish. It also may be called before a load starts or after it
     * finishes.
     *
     * <p>
     *  The best way to use this method is to cancel any loads that have not yet started, but allow those that are in
     *  process to finish since its we typically will want to display the same image in a different view in
     *  the near future.
     * </p>
     */
    public void cancel();
}
