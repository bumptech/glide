package com.bumptech.glide.load.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * A DataFetcher that uses an {@link android.content.ContentResolver} to load data from a {@link
 * android.net.Uri} pointing to a local resource.
 *
 * @param <T> The type of data that will obtained for the given uri (For example, {@link
 *     java.io.InputStream} or {@link android.os.ParcelFileDescriptor}.
 */
public abstract class LocalUriFetcher<T> implements DataFetcher<T> {
  private static final String TAG = "LocalUriFetcher";
  private final Uri uri;
  private final ContentResolver contentResolver;
  private T data;

  /**
   * Opens an input stream for a uri pointing to a local asset. Only certain uris are supported
   *
   * @param contentResolver Any {@link android.content.ContentResolver}.
   * @param uri A Uri pointing to a local asset. This load will fail if the uri isn't openable by
   *     {@link ContentResolver#openInputStream(android.net.Uri)}
   * @see ContentResolver#openInputStream(android.net.Uri)
   */
  // Public API.
  @SuppressWarnings("WeakerAccess")
  public LocalUriFetcher(ContentResolver contentResolver, Uri uri) {
    this.contentResolver = contentResolver;
    this.uri = uri;
  }

  @Override
  public final void loadData(
      @NonNull Priority priority, @NonNull DataCallback<? super T> callback) {
    try {
      data = loadResource(uri, contentResolver);
      callback.onDataReady(data);
    } catch (FileNotFoundException e) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Failed to open Uri", e);
      }
      callback.onLoadFailed(e);
    }
  }

  @Override
  public void cleanup() {
    if (data != null) {
      try {
        close(data);
      } catch (IOException e) {
        // Ignored.
      }
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
   * Returns a concrete data type from the given {@link android.net.Uri} using the given {@link
   * android.content.ContentResolver}.
   */
  protected abstract T loadResource(Uri uri, ContentResolver contentResolver)
      throws FileNotFoundException;

  /**
   * Closes the concrete data type if necessary.
   *
   * <p>Note - We can't rely on the closeable interface because it was added after our min API
   * level. See issue #157.
   *
   * @param data The data to close.
   */
  protected abstract void close(T data) throws IOException;
}
