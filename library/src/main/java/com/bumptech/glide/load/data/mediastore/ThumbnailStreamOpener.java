package com.bumptech.glide.load.data.mediastore;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.ImageHeaderParserUtils;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

class ThumbnailStreamOpener {
  private static final String TAG = "ThumbStreamOpener";
  private static final FileService DEFAULT_SERVICE = new FileService();

  private final FileService service;
  private final ThumbnailQuery query;
  private final ArrayPool byteArrayPool;
  private final ContentResolver contentResolver;
  private final List<ImageHeaderParser> parsers;

  ThumbnailStreamOpener(
      List<ImageHeaderParser> parsers,
      ThumbnailQuery query,
      ArrayPool byteArrayPool,
      ContentResolver contentResolver) {
    this(parsers, DEFAULT_SERVICE, query, byteArrayPool, contentResolver);
  }

  ThumbnailStreamOpener(
      List<ImageHeaderParser> parsers,
      FileService service,
      ThumbnailQuery query,
      ArrayPool byteArrayPool,
      ContentResolver contentResolver) {
    this.service = service;
    this.query = query;
    this.byteArrayPool = byteArrayPool;
    this.contentResolver = contentResolver;
    this.parsers = parsers;
  }

  int getOrientation(Uri uri) {
    InputStream is = null;
    try {
      is = contentResolver.openInputStream(uri);
      return ImageHeaderParserUtils.getOrientation(parsers, is, byteArrayPool);
      // PMD.AvoidCatchingNPE framework method openInputStream can throw NPEs.
    } catch (@SuppressWarnings("PMD.AvoidCatchingNPE") IOException | NullPointerException e) {
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
    return ImageHeaderParser.UNKNOWN_ORIENTATION;
  }

  public InputStream open(Uri uri) throws FileNotFoundException {
    String path = getPath(uri);
    if (TextUtils.isEmpty(path)) {
      return null;
    }

    File file = service.get(path);
    if (!isValid(file)) {
      return null;
    }

    Uri thumbnailUri = Uri.fromFile(file);
    try {
      return contentResolver.openInputStream(thumbnailUri);
      // PMD.AvoidCatchingNPE framework method openInputStream can throw NPEs.
    } catch (
        @SuppressWarnings("PMD.AvoidCatchingNPE")
        NullPointerException e) {
      throw (FileNotFoundException)
          new FileNotFoundException("NPE opening uri: " + uri + " -> " + thumbnailUri).initCause(e);
    }
  }

  @Nullable
  private String getPath(@NonNull Uri uri) {
    Cursor cursor = null;
    try {
      cursor = query.query(uri);
      if (cursor != null && cursor.moveToFirst()) {
        return cursor.getString(0);
      } else {
        return null;
      }
    } catch (SecurityException e) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Failed to query for thumbnail for Uri: " + uri, e);
      }
      return null;
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
  }

  private boolean isValid(File file) {
    return service.exists(file) && 0 < service.length(file);
  }
}
