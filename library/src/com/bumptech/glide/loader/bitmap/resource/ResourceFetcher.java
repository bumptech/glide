package com.bumptech.glide.loader.bitmap.resource;

import com.bumptech.glide.loader.bitmap.model.ModelLoader;
import com.bumptech.glide.resize.Metadata;

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
     * @param metadata Load related metadata that the fetcher can use to adjust how it obtains it's resource.
     */
    public T loadResource(Metadata metadata) throws Exception;

    /**
     * A method that will be called by an {@link com.bumptech.glide.presenter.ImagePresenter} when a load is no longer
     * relevant (because we now want to load a different image into the view). This method does not need to guarantee
     * that any in process loads do not finish. It also may be called before a load starts or after it finishes.
     *
     * The best way to use this method is to cancel any loads that have not yet started, but allow those that are in
     * process to finish since its we typically will want to display the same image in a different view in
     * the near future.
     */
    public void cancel();
}
