package com.bumptech.glide.load.data.mediastore;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.data.ExifOrientationStream;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link DataFetcher} implementation for {@link InputStream}s that loads data from thumbnail
 * files obtained from the {@link MediaStore}.
 */
@SuppressWarnings("PMD.FieldDeclarationsShouldBeAtStartOfClass")
public class ThumbFetcher implements DataFetcher<InputStream> {
  private static final String TAG = "MediaStoreThumbFetcher";
  private final Uri mediaStoreImageUri;
  private final ThumbnailStreamOpener opener;
  private InputStream inputStream;

  public static ThumbFetcher buildImageFetcher(Context context, Uri uri) {
    return build(context, uri, new ImageThumbnailQuery(context.getContentResolver()));
  }

  public static ThumbFetcher buildVideoFetcher(Context context, Uri uri) {
    return build(context, uri, new VideoThumbnailQuery(context.getContentResolver()));
  }

  private static ThumbFetcher build(Context context, Uri uri, ThumbnailQuery query) {
    ArrayPool byteArrayPool = Glide.get(context).getArrayPool();
    List<File> blockedRoots = getBlockedRoots(context);
    ThumbnailStreamOpener opener =
        new ThumbnailStreamOpener(
            Glide.get(context).getRegistry().getImageHeaderParsers(),
            query,
            byteArrayPool,
            context.getContentResolver(),
            blockedRoots);
    return new ThumbFetcher(uri, opener);
  }

  private static List<File> getBlockedRoots(Context context) {
    List<File> blockedRoots = new ArrayList<>();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      File dataDir = context.getDataDir();
      if (dataDir != null) {
        blockedRoots.add(dataDir);
      }
      Context deviceProtectedContext = context.createDeviceProtectedStorageContext();
      if (deviceProtectedContext != null) {
        File deviceProtectedDataDir = deviceProtectedContext.getDataDir();
        if (deviceProtectedDataDir != null) {
          blockedRoots.add(deviceProtectedDataDir);
        }
      }
    } else if (context.getApplicationInfo() != null
        && context.getApplicationInfo().dataDir != null) {
      blockedRoots.add(new File(context.getApplicationInfo().dataDir));
    }

    File[] externalFilesDirs = context.getExternalFilesDirs(null);
    if (externalFilesDirs != null) {
      for (File dir : externalFilesDirs) {
        if (dir != null) {
          blockedRoots.add(dir);
        }
      }
    }

    File[] externalCacheDirs = context.getExternalCacheDirs();
    if (externalCacheDirs != null) {
      for (File dir : externalCacheDirs) {
        if (dir != null) {
          blockedRoots.add(dir);
        }
      }
    }

    return blockedRoots;
  }

  @VisibleForTesting
  ThumbFetcher(Uri mediaStoreImageUri, ThumbnailStreamOpener opener) {
    this.mediaStoreImageUri = mediaStoreImageUri;
    this.opener = opener;
  }

  @Override
  public void loadData(
      @NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
    try {
      inputStream = openThumbInputStream();
      callback.onDataReady(inputStream);
    } catch (FileNotFoundException e) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Failed to find thumbnail file", e);
      }
      callback.onLoadFailed(e);
    }
  }

  private InputStream openThumbInputStream() throws FileNotFoundException {
    InputStream result = opener.open(mediaStoreImageUri);

    int orientation = -1;
    if (result != null) {
      orientation = opener.getOrientation(mediaStoreImageUri);
    }

    if (orientation != -1) {
      result = new ExifOrientationStream(result, orientation);
    }
    return result;
  }

  @Override
  public void cleanup() {
    if (inputStream != null) {
      try {
        inputStream.close();
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
  public Class<InputStream> getDataClass() {
    return InputStream.class;
  }

  @NonNull
  @Override
  public DataSource getDataSource() {
    return DataSource.LOCAL;
  }

  static class VideoThumbnailQuery implements ThumbnailQuery {

    private final ContentResolver contentResolver;

    VideoThumbnailQuery(ContentResolver contentResolver) {
      this.contentResolver = contentResolver;
    }

    private static final String[] PATH_PROJECTION = {MediaStore.Video.Thumbnails.DATA};
    private static final String PATH_SELECTION =
        MediaStore.Video.Thumbnails.KIND
            + " = "
            + MediaStore.Video.Thumbnails.MINI_KIND
            + " AND "
            + MediaStore.Video.Thumbnails.VIDEO_ID
            + " = ?";

    @Override
    public Cursor query(Uri uri) {
      String videoId = uri.getLastPathSegment();
      return contentResolver.query(
          MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI,
          PATH_PROJECTION,
          PATH_SELECTION,
          new String[] {videoId},
          null /*sortOrder*/);
    }
  }

  static class ImageThumbnailQuery implements ThumbnailQuery {

    private final ContentResolver contentResolver;

    ImageThumbnailQuery(ContentResolver contentResolver) {
      this.contentResolver = contentResolver;
    }

    private static final String[] PATH_PROJECTION = {
      MediaStore.Images.Thumbnails.DATA,
    };
    private static final String PATH_SELECTION =
        MediaStore.Images.Thumbnails.KIND
            + " = "
            + MediaStore.Images.Thumbnails.MINI_KIND
            + " AND "
            + MediaStore.Images.Thumbnails.IMAGE_ID
            + " = ?";

    @Override
    public Cursor query(Uri uri) {
      String imageId = uri.getLastPathSegment();
      return contentResolver.query(
          MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
          PATH_PROJECTION,
          PATH_SELECTION,
          new String[] {imageId},
          null /*sortOrder*/);
    }
  }
}
