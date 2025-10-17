package com.bumptech.glide.load.model.stream;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.data.mediastore.MediaStoreUtil;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.signature.ObjectKey;
import com.bumptech.glide.util.Synthetic;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Best effort attempt to work around various Q storage states and bugs.
 *
 * <p>In particular, HEIC images on Q cannot be decoded if they've gone through Android's exif
 * redaction, due to a bug in the implementation that corrupts the file. To avoid the issue, we need
 * to get at the un-redacted File. There are two ways we can do so:
 *
 * <ul>
 *   <li>MediaStore.setRequireOriginal
 *   <li>Querying for and opening the file via the underlying file path, rather than via {@code
 *       ContentResolver}
 * </ul>
 *
 * <p>MediaStore.setRequireOriginal will only work for applications that target Q and request and
 * currently have {@link android.Manifest.permission#ACCESS_MEDIA_LOCATION}. It's the simplest
 * change to make, but it covers the fewest applications.
 *
 * <p>Querying for the file path and opening the file directly works for applications that do not
 * target Q and for applications that do target Q but that opt in to legacy storage mode. Other
 * options are theoretically available for applications that do not target Q, but due to other bugs,
 * the only consistent way to get unredacted files is via the file system.
 *
 * <p>This class does not fix applications that target Q, do not opt in to legacy storage and that
 * don't have {@link android.Manifest.permission#ACCESS_MEDIA_LOCATION}.
 *
 * <p>Avoid using this class directly, it may be removed in any future version of Glide.
 *
 * @param <DataT> The type of data this loader will load ({@link InputStream}, {@link
 *     ParcelFileDescriptor}).
 */
@RequiresApi(Build.VERSION_CODES.Q)
public final class QMediaStoreUriLoader<DataT> implements ModelLoader<Uri, DataT> {
  private final Context context;
  private final ModelLoader<File, DataT> fileDelegate;
  private final ModelLoader<Uri, DataT> uriDelegate;
  private final Class<DataT> dataClass;

  @SuppressWarnings("WeakerAccess")
  @Synthetic
  QMediaStoreUriLoader(
      Context context,
      ModelLoader<File, DataT> fileDelegate,
      ModelLoader<Uri, DataT> uriDelegate,
      Class<DataT> dataClass) {
    this.context = context.getApplicationContext();
    this.fileDelegate = fileDelegate;
    this.uriDelegate = uriDelegate;
    this.dataClass = dataClass;
  }

  @Override
  public LoadData<DataT> buildLoadData(
      @NonNull Uri uri, int width, int height, @NonNull Options options) {
    return new LoadData<>(
        new ObjectKey(uri),
        new QMediaStoreUriFetcher<>(
            context, fileDelegate, uriDelegate, uri, width, height, options, dataClass));
  }

  @Override
  public boolean handles(@NonNull Uri uri) {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && MediaStoreUtil.isMediaStoreUri(uri);
  }

  private static final class QMediaStoreUriFetcher<DataT> implements DataFetcher<DataT> {
    private static final String[] PROJECTION = new String[] {MediaStore.MediaColumns.DATA};

    private final Context context;
    private final ModelLoader<File, DataT> fileDelegate;
    private final ModelLoader<Uri, DataT> uriDelegate;
    private final Uri uri;
    private final int width;
    private final int height;
    private final Options options;
    private final Class<DataT> dataClass;

    private volatile boolean isCancelled;
    @Nullable private volatile DataFetcher<DataT> delegate;

    QMediaStoreUriFetcher(
        Context context,
        ModelLoader<File, DataT> fileDelegate,
        ModelLoader<Uri, DataT> uriDelegate,
        Uri uri,
        int width,
        int height,
        Options options,
        Class<DataT> dataClass) {
      this.context = context.getApplicationContext();
      this.fileDelegate = fileDelegate;
      this.uriDelegate = uriDelegate;
      this.uri = uri;
      this.width = width;
      this.height = height;
      this.options = options;
      this.dataClass = dataClass;
    }

    @Override
    public void loadData(
        @NonNull Priority priority, @NonNull DataCallback<? super DataT> callback) {
      try {
        DataFetcher<DataT> local = buildDelegateFetcher();
        if (local == null) {
          callback.onLoadFailed(
              new IllegalArgumentException("Failed to build fetcher for: " + uri));
          return;
        }
        delegate = local;
        if (isCancelled) {
          cancel();
        } else {
          local.loadData(priority, callback);
        }
      } catch (FileNotFoundException e) {
        callback.onLoadFailed(e);
      }
    }

    @Nullable
    private DataFetcher<DataT> buildDelegateFetcher() throws FileNotFoundException {
      LoadData<DataT> result = buildDelegateData();
      return result != null ? result.fetcher : null;
    }

    @Nullable
    private LoadData<DataT> buildDelegateData() throws FileNotFoundException {
      if (Environment.isExternalStorageLegacy()) {
        return fileDelegate.buildLoadData(queryForFilePath(uri), width, height, options);
      } else {
        // Android Picker uris have MediaStore authority and does not accept requireOriginal.
        if (MediaStoreUtil.isAndroidPickerUri(uri)) {
          return uriDelegate.buildLoadData(uri, width, height, options);
        }

        Uri toLoad = isAccessMediaLocationGranted() ? MediaStore.setRequireOriginal(uri) : uri;
        return uriDelegate.buildLoadData(toLoad, width, height, options);
      }
    }

    @Override
    public void cleanup() {
      DataFetcher<DataT> local = delegate;
      if (local != null) {
        local.cleanup();
      }
    }

    @Override
    public void cancel() {
      isCancelled = true;
      DataFetcher<DataT> local = delegate;
      if (local != null) {
        local.cancel();
      }
    }

    @NonNull
    @Override
    public Class<DataT> getDataClass() {
      return dataClass;
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
      return DataSource.LOCAL;
    }

    @NonNull
    private File queryForFilePath(Uri uri) throws FileNotFoundException {
      Cursor cursor = null;
      try {
        cursor =
            context
                .getContentResolver()
                .query(
                    uri,
                    PROJECTION,
                    /* selection= */ null,
                    /* selectionArgs= */ null,
                    /* sortOrder= */ null);
        if (cursor == null || !cursor.moveToFirst()) {
          throw new FileNotFoundException("Failed to media store entry for: " + uri);
        }
        String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA));
        if (TextUtils.isEmpty(path)) {
          throw new FileNotFoundException("File path was empty in media store for: " + uri);
        }
        return new File(path);
      } finally {
        if (cursor != null) {
          cursor.close();
        }
      }
    }

    private boolean isAccessMediaLocationGranted() {
      return context.checkSelfPermission(android.Manifest.permission.ACCESS_MEDIA_LOCATION)
          == PackageManager.PERMISSION_GRANTED;
    }
  }

  /** Factory for {@link InputStream}. */
  @RequiresApi(Build.VERSION_CODES.Q)
  public static final class InputStreamFactory extends Factory<InputStream> {
    public InputStreamFactory(Context context) {
      super(context, InputStream.class);
    }
  }

  /** Factory for {@link ParcelFileDescriptor}. */
  @RequiresApi(Build.VERSION_CODES.Q)
  public static final class FileDescriptorFactory extends Factory<ParcelFileDescriptor> {
    public FileDescriptorFactory(Context context) {
      super(context, ParcelFileDescriptor.class);
    }
  }

  private abstract static class Factory<DataT> implements ModelLoaderFactory<Uri, DataT> {

    private final Context context;
    private final Class<DataT> dataClass;

    Factory(Context context, Class<DataT> dataClass) {
      this.context = context;
      this.dataClass = dataClass;
    }

    @NonNull
    @Override
    public final ModelLoader<Uri, DataT> build(@NonNull MultiModelLoaderFactory multiFactory) {
      return new QMediaStoreUriLoader<>(
          context,
          multiFactory.build(File.class, dataClass),
          multiFactory.build(Uri.class, dataClass),
          dataClass);
    }

    @Override
    public final void teardown() {
      // Do nothing.
    }
  }
}
