package com.bumptech.glide.load.data;

import com.bumptech.glide.Priority;

/**
 * An interface for lazily retrieving data that can be used to load a resource. A new instance is created per
 * resource load by {@link com.bumptech.glide.load.model.ModelLoader}. {@link #loadData(Priority)} may or may not be
 * called for any given load depending on whether or not the corresponding resource is cached. Cancel also may or may
 * not be called. If {@link #loadData(Priority)} is called, then so {@link #cleanup()} will be called.
 *
 * @param <T> The type of data to be loaded (InputStream, byte[], File etc).
 */
public interface DataFetcher<T> {

    /**
     * Asynchronously fetch data from which a resource can be decoded. This will always be called on
     * background thread so it is safe to perform long running tasks here. Any third party libraries called
     * must be thread safe since this method will be called from a thread in a
     * {@link java.util.concurrent.ExecutorService} that may have more than one background thread.
     *
     * This method will only be called when the corresponding resource is not in the cache.
     *
     * <p>
     *     Note - this method will be run on a background thread so blocking I/O is safe.
     * </p>
     *
     * @param priority The priority with which the request should be completed.
     * @see #cleanup() where the data retuned will be cleaned up
     */
    T loadData(Priority priority) throws Exception;

    /**
     * Cleanup or recycle any resources used by this data fetcher. This method will be called in a finally block
     * after the data returned by {@link #loadData(Priority)} has been decoded by the
     * {@link com.bumptech.glide.load.ResourceDecoder}.
     *
     * <p>
     *     Note - this method will be run on a background thread so blocking I/O is safe.
     * </p>
     *
     */
    void cleanup();

    /**
     * Returns a string uniquely identifying the data that this fetcher will fetch including the specific size.
     *
     * <p>
     *     A hash of the bytes of the data that will be fetched is the ideal id but since that is in many cases
     *     impractical, urls, file paths, and uris are normally sufficient.
     * </p>
     *
     * <p>
     *     Note - this method will be run on the main thread so it should not perform blocking operations and should
     *     finish quickly.
     * </p>
     */
    String getId();

    /**
     * A method that will be called when a load is no longer relevant and has been cancelled. This method does not need
     * to guarantee that any in process loads do not finish. It also may be called before a load starts or after it
     * finishes.
     *
     * <p>
     *  The best way to use this method is to cancel any loads that have not yet started, but allow those that are in
     *  process to finish since its we typically will want to display the same resource in a different view in
     *  the near future.
     * </p>
     *
     * <p>
     *     Note - this method will be run on the main thread so it should not perform blocking operations and should
     *     finish quickly.
     * </p>
     */
    void cancel();
}
