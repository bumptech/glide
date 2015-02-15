package com.bumptech.glide.load.data.mediastore;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;

import java.io.IOException;
import java.io.InputStream;

/**
 * A {@link DataFetcher} implementation for {@link InputStream}s that loads data from thumbnail
 * files obtained from the {@link MediaStore}.
 */
public class ThumbFetcher implements DataFetcher<InputStream> {
  private final Context context;
  private final Uri mediaStoreImageUri;
  private final ThumbnailStreamOpener opener;
  private InputStream inputStream;

  public static ThumbFetcher buildImageFetcher(Context context, Uri uri) {
    return build(context, uri, new ImageThumbnailQuery());
  }

  public static ThumbFetcher buildVideoFetcher(Context context, Uri uri) {
    return build(context, uri, new VideoThumbnailQuery());
  }

  private static ThumbFetcher build(Context context, Uri uri, ThumbnailQuery query) {
    return new ThumbFetcher(context, uri, new ThumbnailStreamOpener(query));
  }

  // Visible for testing.
  ThumbFetcher(Context context, Uri mediaStoreImageUri, ThumbnailStreamOpener opener) {
    this.context = context;
    this.mediaStoreImageUri = mediaStoreImageUri;
    this.opener = opener;
  }

  @Override
  public InputStream loadData(Priority priority) throws IOException {
    inputStream = opener.open(context, mediaStoreImageUri);
    return inputStream;
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
  public String getId() {
    return mediaStoreImageUri.toString();
  }

  @Override
  public void cancel() {
    // Do nothing.
  }

  @Override
  public Class<InputStream> getDataClass() {
    return InputStream.class;
  }

  @Override
  public DataSource getDataSource() {
    return DataSource.LOCAL;
  }

  // Visible for testing.
  static class VideoThumbnailQuery implements ThumbnailQuery {

    @Override
    public Cursor query(Context context, Uri uri) {
      String id = uri.getLastPathSegment();
      return context.getContentResolver().query(MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI,
          new String[] { MediaStore.Video.Thumbnails.DATA },
          MediaStore.Video.Thumbnails.VIDEO_ID + " = ? AND " + MediaStore.Video.Thumbnails.KIND
              + " = ?", new String[] { id, String.valueOf(MediaStore.Video.Thumbnails.MINI_KIND) },
          null);
    }
  }

  // Visible for testing.
  static class ImageThumbnailQuery implements ThumbnailQuery {

    @Override
    public Cursor query(Context context, Uri uri) {
      String id = uri.getLastPathSegment();
      return context.getContentResolver().query(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
          new String[] { MediaStore.Images.Thumbnails.DATA },
          MediaStore.Images.Thumbnails.IMAGE_ID + " = ? AND " + MediaStore.Images.Thumbnails.KIND
              + " = ?", new String[] { id, String.valueOf(MediaStore.Images.Thumbnails.MINI_KIND) },
          null);
    }
  }
}
