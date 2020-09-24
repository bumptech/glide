package com.bumptech.glide.load.data.mediastore;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

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
    ThumbnailStreamOpener opener =
        new ThumbnailStreamOpener(
            Glide.get(context).getRegistry().getImageHeaderParsers(),
            query,
            byteArrayPool,
            context.getContentResolver());
    return new ThumbFetcher(uri, opener);
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
