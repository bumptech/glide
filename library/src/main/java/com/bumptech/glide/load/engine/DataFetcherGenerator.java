package com.bumptech.glide.load.engine;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.data.DataFetcher;

/**
 * Generates a series of {@link com.bumptech.glide.load.data.DataFetcher DataFetchers} using
 * registered {@link com.bumptech.glide.load.model.ModelLoader ModelLoaders} and a model.
 */
interface DataFetcherGenerator {
  /**
   * Called when the generator has finished loading data from a
   * {@link com.bumptech.glide.load.data.DataFetcher}.
   */
  interface FetcherReadyCallback {

    /**
     * Requests that we call startNext() again on a Glide owned thread.
     */
    void reschedule();

    /**
     * Notifies the callback that the load is complete.
     *
     * @param sourceKey The id of the loaded data.
     * @param data The loaded data, or null if the load failed.
     * @param fetcher The data fetcher we attempted to load from.
     */
    void onDataFetcherReady(Key sourceKey, Object data, DataFetcher<?> fetcher,
        DataSource dataSource);
  }

  /**
   * Attempts to a single new {@link com.bumptech.glide.load.data.DataFetcher} and returns true if
   * a {@link com.bumptech.glide.load.data.DataFetcher} was started, and false otherwise.
   */
  boolean startNext();

  /**
   * Attempts to cancel the currently running fetcher.
   *
   * <p> This will be called on the main thread and should complete quickly. </p>
   */
  void cancel();
}
