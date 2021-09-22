package com.bumptech.glide.load.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;

/**
 * Lazily retrieves data that can be used to load a resource.
 *
 * <p>A new instance is created per resource load by {@link
 * com.bumptech.glide.load.model.ModelLoader}. {@link #loadData(com.bumptech.glide.Priority,
 * com.bumptech.glide.load.data.DataFetcher.DataCallback)} may or may not be called for any given
 * load depending on whether or not the corresponding resource is cached. Cancel also may or may not
 * be called. If {@link #loadData(com.bumptech.glide.Priority,
 * com.bumptech.glide.load.data.DataFetcher.DataCallback)}} is called, then so {@link #cleanup()}
 * will be called.
 *
 * @param <T> The type of data to be loaded (InputStream, byte[], File etc).
 */
public interface DataFetcher<T> {

  /**
   * Callback that must be called when data has been loaded and is available, or when the load
   * fails.
   *
   * @param <T> The type of data that will be loaded.
   */
  interface DataCallback<T> {

    /**
     * Called with the loaded data if the load succeeded, or with {@code null} if the load failed.
     */
    void onDataReady(@Nullable T data);

    /**
     * Called when the load fails.
     *
     * @param e a non-null {@link Exception} indicating why the load failed.
     */
    void onLoadFailed(@NonNull Exception e);
  }

  /**
   * Fetch data from which a resource can be decoded.
   *
   * <p>This will always be called on background thread so it is safe to perform long running tasks
   * here. Any third party libraries called must be thread safe (or move the work to another thread)
   * since this method will be called from a thread in a {@link
   * java.util.concurrent.ExecutorService} that may have more than one background thread. You
   * <b>MUST</b> use the {@link DataCallback} once the request is complete.
   *
   * <p>You are free to move the fetch work to another thread and call the callback from there.
   *
   * <p>This method will only be called when the corresponding resource is not in the cache.
   *
   * <p>Note - this method will be run on a background thread so blocking I/O is safe.
   *
   * @param priority The priority with which the request should be completed.
   * @param callback The callback to use when the request is complete
   * @see #cleanup() where the data retuned will be cleaned up
   */
  void loadData(@NonNull Priority priority, @NonNull DataCallback<? super T> callback);

  /**
   * Cleanup or recycle any resources used by this data fetcher. This method will be called in a
   * finally block after the data provided by {@link #loadData(com.bumptech.glide.Priority,
   * com.bumptech.glide.load.data.DataFetcher.DataCallback)} has been decoded by the {@link
   * com.bumptech.glide.load.ResourceDecoder}.
   *
   * <p>Note - this method will be run on a background thread so blocking I/O is safe.
   */
  void cleanup();

  /**
   * A method that will be called when a load is no longer relevant and has been cancelled. This
   * method does not need to guarantee that any in process loads do not finish. It also may be
   * called before a load starts or after it finishes.
   *
   * <p>The best way to use this method is to cancel any loads that have not yet started, but allow
   * those that are in process to finish since its we typically will want to display the same
   * resource in a different view in the near future.
   *
   * <p>Note - this method will be run on the main thread so it should not perform blocking
   * operations and should finish quickly.
   */
  void cancel();

  /** Returns the class of the data this fetcher will attempt to obtain. */
  @NonNull
  Class<T> getDataClass();

  /** Returns the {@link com.bumptech.glide.load.DataSource} this fetcher will return data from. */
  @NonNull
  DataSource getDataSource();
}
