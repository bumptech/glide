package com.bumptech.glide.load.data.mediastore;

import android.content.ContentResolver;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.ext.SdkExtensions;
import android.provider.MediaStore;
import androidx.annotation.ChecksSdkIntAtLeast;
import androidx.annotation.RequiresExtension;
import com.bumptech.glide.request.target.Target;
import java.io.FileNotFoundException;

/** Utility classes for interacting with the media store. */
public final class MediaStoreUtil {
  public static final int MIN_EXTENSION_VERSION_FOR_OPEN_FILE_APIS = 17;
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

  @ChecksSdkIntAtLeast(api = MIN_EXTENSION_VERSION_FOR_OPEN_FILE_APIS, extension = VERSION_CODES.R)
  public static boolean isMediaStoreOpenFileApisAvailable() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        && SdkExtensions.getExtensionVersion(Build.VERSION_CODES.R)
            >= MIN_EXTENSION_VERSION_FOR_OPEN_FILE_APIS;
  }

  @RequiresExtension(
      extension = VERSION_CODES.R,
      version = MIN_EXTENSION_VERSION_FOR_OPEN_FILE_APIS)
  public static AssetFileDescriptor openAssetFileDescriptor(
      Uri uri, ContentResolver contentResolver) throws FileNotFoundException {
    return MediaStore.openAssetFileDescriptor(contentResolver, uri, "r", null);
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
