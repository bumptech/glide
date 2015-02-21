package com.bumptech.glide.load.data;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;

import java.io.IOException;

/**
 * Lazily retrieves data that can be used to load a resource.
 *
 * <p> A new instance is
 * created per resource load by {@link com.bumptech.glide.load.model.ModelLoader}. {@link
 * #loadData(com.bumptech.glide.Priority, com.bumptech.glide.load.data.DataFetcher.DataCallback)}
 * may or may not be called for any given load depending on whether or not the corresponding
 * resource is cached. Cancel also may or may not be called. If
 * {@link #loadData(com.bumptech.glide.Priority,
 * com.bumptech.glide.load.data.DataFetcher.DataCallback)}} is called, then so {@link #cleanup()}
 * will be called. </p>
 *
 * @param <T> The type of data to be loaded (InputStream, byte[], File etc).
 */
public interface DataFetcher<T> {

  /**
   * Callback that should be called when data has been loaded and is available.
   * @param <T> The type of data that will be loaded.
   */
  interface DataCallback<T> {
    void onDataReady(T data);
  }

  /**
   * Synchronously fetch data from which a resource can be decoded.
   *
   * <p> This will always be called on
   * background thread so it is safe to perform long running tasks here. Any third party libraries
   * called must be thread safe since this method will be called from a thread in a {@link
   * java.util.concurrent.ExecutorService} that may have more than one background thread. </p>
   *
   * <p> This method will only be called when the corresponding resource is not in the cache. </p>
   *
   * <p> Note - this method will be run on a background thread so blocking I/O is safe. </p>
   *
   * @param priority The priority with which the request should be completed.
   * @see #cleanup() where the data retuned will be cleaned up
   */
  void loadData(Priority priority, DataCallback<? super T> callback) throws IOException;

  /**
   * Cleanup or recycle any resources used by this data fetcher. This method will be called in a
   * finally block after the data provided by {@link #loadData(com.bumptech.glide.Priority,
   * com.bumptech.glide.load.data.DataFetcher.DataCallback)} has been decoded by the
   * {@link com.bumptech.glide.load.ResourceDecoder}.
   *
   * <p> Note - this method will be run on a background thread so blocking I/O is safe. </p>
   */
  void cleanup();

  /**
   * Returns a string uniquely identifying the data that this fetcher will fetch including the
   * specific size.
   *
   * <p> A hash of the bytes of the data that will be fetched is the ideal id but since that is in
   * many cases impractical, urls, file paths, and uris are normally sufficient. </p>
   *
   * <p> Note - this method will be run on the main thread so it should not perform blocking
   * operations and should finish quickly. </p>
   */
  String getId();

  /**
   * A method that will be called when a load is no longer relevant and has been cancelled. This
   * method does not need to guarantee that any in process loads do not finish. It also may be
   * called before a load starts or after it finishes.
   *
   * <p> The best way to use this method is to cancel any loads that have not yet started, but allow
   * those that are in process to finish since its we typically will want to display the same
   * resource in a different view in the near future. </p>
   *
   * <p> Note - this method will be run on the main thread so it should not perform blocking
   * operations and should finish quickly. </p>
   */
  void cancel();

  Class<T> getDataClass();

  /**
   * Returns the {@link com.bumptech.glide.load.DataSource} this fetcher will return data from.
   */
  DataSource getDataSource();
}
