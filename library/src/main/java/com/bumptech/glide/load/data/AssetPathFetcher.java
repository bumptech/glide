package com.bumptech.glide.load.data;

import android.content.res.AssetManager;
import android.util.Log;
import androidx.annotation.NonNull;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import java.io.IOException;

/**
 * An abstract class for obtaining data for an asset path using an {@link
 * android.content.res.AssetManager}.
 *
 * @param <T> The type of data obtained from the asset path (InputStream, FileDescriptor etc).
 */
public abstract class AssetPathFetcher<T> implements DataFetcher<T> {
  private static final String TAG = "AssetPathFetcher";
  private final String assetPath;
  private final AssetManager assetManager;
  private T data;

  // Public API.
  @SuppressWarnings("WeakerAccess")
  public AssetPathFetcher(AssetManager assetManager, String assetPath) {
    this.assetManager = assetManager;
    this.assetPath = assetPath;
  }

  @Override
  public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super T> callback) {
    try {
      data = loadResource(assetManager, assetPath);
      callback.onDataReady(data);
    } catch (IOException e) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Failed to load data from asset manager", e);
      }
      callback.onLoadFailed(e);
    }
  }

  @Override
  public void cleanup() {
    if (data == null) {
      return;
    }
    try {
      close(data);
    } catch (IOException e) {
      // Ignored.
    }
  }

  @Override
  public void cancel() {
    // Do nothing.
  }

  @NonNull
  @Override
  public DataSource getDataSource() {
    return DataSource.LOCAL;
  }

  /**
   * Opens the given asset path with the given {@link android.content.res.AssetManager} and returns
   * the concrete data type returned by the AssetManager.
   *
   * @param assetManager An AssetManager to use to open the given path.
   * @param path A string path pointing to a resource in assets to open.
   */
  protected abstract T loadResource(AssetManager assetManager, String path) throws IOException;

  /**
   * Closes the concrete data type if necessary.
   *
   * @param data The data to close.
   */
  protected abstract void close(T data) throws IOException;
}
