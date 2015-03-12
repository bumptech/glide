package com.bumptech.glide.load.data.mediastore;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.bumptech.glide.load.engine.bitmap_recycle.ByteArrayPool;
import com.bumptech.glide.load.resource.bitmap.ImageHeaderParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

class ThumbnailStreamOpener {
  private static final String TAG = "ThumbStreamOpener";
  private static final FileService DEFAULT_SERVICE = new FileService();
  private final FileService service;
  private final ThumbnailQuery query;
  private final ByteArrayPool byteArrayPool;

  public ThumbnailStreamOpener(ThumbnailQuery query, ByteArrayPool byteArrayPool) {
    this(DEFAULT_SERVICE, query, byteArrayPool);
  }

  public ThumbnailStreamOpener(FileService service, ThumbnailQuery query,
      ByteArrayPool byteArrayPool) {
    this.service = service;
    this.query = query;
    this.byteArrayPool = byteArrayPool;
  }

  public int getOrientation(Context context, Uri uri) {
    int orientation = -1;
    InputStream is = null;
    try {
      is = context.getContentResolver().openInputStream(uri);
      orientation = new ImageHeaderParser(is, byteArrayPool).getOrientation();
    } catch (IOException e) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
          Log.d(TAG, "Failed to open uri: " + uri, e);
      }
    } finally {
      if (is != null) {
          try {
              is.close();
          } catch (IOException e) {
              // Ignored.
          }
      }
    }
      return orientation;
  }

  public InputStream open(Context context, Uri uri) throws FileNotFoundException {
    Uri thumbnailUri = null;
    InputStream inputStream = null;

    final Cursor cursor = query.query(context, uri);
    try {
      if (cursor == null || !cursor.moveToFirst()) {
        return null;
      }
      String path = cursor.getString(0);
      if (TextUtils.isEmpty(path)) {
        return null;
      }

      File file = service.get(path);
      if (service.exists(file) && service.length(file) > 0) {
        thumbnailUri = Uri.fromFile(file);
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    if (thumbnailUri != null) {
      inputStream = context.getContentResolver().openInputStream(thumbnailUri);
    }
    return inputStream;
  }
}
