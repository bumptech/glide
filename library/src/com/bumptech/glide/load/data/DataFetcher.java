package com.bumptech.glide.load.data;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.model.ModelLoader;

/**
 * A base class for lazily and retrieving data that can be used to load a resource. A new instance is created per image
 * load by {@link ModelLoader}. {@link #loadData(Priority)} may or may not be called for any given load depending on
 * whether or not the corresponding image is cached. Cancel also may or may not be called. If
 * {@link #loadData(Priority)} is called, then so {@link #cleanup()} will be called.
 *
 * @param <T> The type of data to be loaded.
 */
public interface DataFetcher<T> {

    /**
     * Asynchronously fetch data from which a resource can be decoded. This will always be called on
     * background thread so it is safe to perform long running tasks here. Any third party libraries called
     * must be thread safe since this method will be called from a thread in a
     * {@link java.util.concurrent.ExecutorService} that may have more than one background thread.
     *
     * This method will only be called when the corresponding image is not in the cache.
     *
     * @param priority The priority with which the request should be completed.
     */
    public T loadData(Priority priority) throws Exception;

    /**
     * Cleanup or recycle any resources used by this data fetcher. This method will be called in a finally block
     * after the data returned by {@link #loadData(Priority)} has been decoded by the {@link ResourceDecoder}.
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
