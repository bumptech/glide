package com.bumptech.glide.load.data.mediastore;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.MediaStore;
import com.bumptech.glide.request.target.Target;

/** Utility classes for interacting with the media store. */
public final class MediaStoreUtil {
  private static final int MINI_THUMB_WIDTH = 512;
  private static final int MINI_THUMB_HEIGHT = 384;

  private MediaStoreUtil() {
    // Utility class.
  }

  public static boolean isMediaStoreUri(Uri uri) {
    return uri != null
        && ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())
        && MediaStore.AUTHORITY.equals(uri.getAuthority());
  }

  // Android picker uris contain a "picker" segment:
  // https://android.googlesource.com/platform/packages/providers/MediaProvider/+/refs/heads/master/src/com/android/providers/media/PickerUriResolver.java#58
  public static boolean isAndroidPickerUri(Uri uri) {
    return isMediaStoreUri(uri) && uri.getPathSegments().contains("picker");
  }

  private static boolean isVideoUri(Uri uri) {
    return uri.getPathSegments().contains("video");
  }

  public static boolean isMediaStoreVideoUri(Uri uri) {
    return isMediaStoreUri(uri) && isVideoUri(uri);
  }

  public static boolean isMediaStoreImageUri(Uri uri) {
    return isMediaStoreUri(uri) && !isVideoUri(uri);
  }

  public static boolean isThumbnailSize(int width, int height) {
    return width != Target.SIZE_ORIGINAL
        && height != Target.SIZE_ORIGINAL
        && width <= MINI_THUMB_WIDTH
        && height <= MINI_THUMB_HEIGHT;
  }
}
