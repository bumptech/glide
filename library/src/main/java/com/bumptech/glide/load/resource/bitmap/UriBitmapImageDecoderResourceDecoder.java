package com.bumptech.glide.load.resource.bitmap;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.ImageDecoder.Source;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.webkit.MimeTypeMap;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import java.io.IOException;
import java.util.Locale;

/** Decodes {@link Bitmap}s from {@link Uri}s using {@link ImageDecoder}. */
@RequiresApi(Build.VERSION_CODES.P)
public final class UriBitmapImageDecoderResourceDecoder implements ResourceDecoder<Uri, Bitmap> {
  private static final String TAG = "UriBitmapDecoder";
  private final Context context;
  private final BitmapImageDecoderResourceDecoder wrapped = new BitmapImageDecoderResourceDecoder();

  public UriBitmapImageDecoderResourceDecoder(@NonNull Context context) {
    this.context = context.getApplicationContext();
  }

  @Override
  public boolean handles(@NonNull Uri uri, @NonNull Options options) throws IOException {
    String scheme = uri.getScheme();
    boolean isSupportedScheme =
        ContentResolver.SCHEME_CONTENT.equals(scheme)
            || ContentResolver.SCHEME_FILE.equals(scheme)
            || ContentResolver.SCHEME_ANDROID_RESOURCE.equals(scheme);
    if (!isSupportedScheme) {
      return false;
    }
    String mimeType = getMimeType(uri);
    if (mimeType == null) {
      // ContentResolver.getType() can return null for resources in tests (Robolectric) or for some
      // raw resources. We want to be lenient and handle them as they are internal to the app,
      // but we reject null MIME types for content/file URIs to be safe.
      return ContentResolver.SCHEME_ANDROID_RESOURCE.equals(scheme);
    }
    return mimeType.startsWith("image/") && !mimeType.equals("image/gif");
  }

  @Nullable
  private String getMimeType(@NonNull Uri uri) {
    String mimeType = context.getContentResolver().getType(uri);
    if (mimeType == null && ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
      String lastSegment = uri.getLastPathSegment();
      if (lastSegment != null) {
        int lastDot = lastSegment.lastIndexOf('.');
        if (lastDot != -1) {
          String extension = lastSegment.substring(lastDot + 1);
          mimeType =
              MimeTypeMap.getSingleton()
                  .getMimeTypeFromExtension(extension.toLowerCase(Locale.ROOT));
        }
      }
    }
    return mimeType;
  }

  @Override
  public Resource<Bitmap> decode(@NonNull Uri uri, int width, int height, @NonNull Options options)
      throws IOException {
    Source source = ImageDecoder.createSource(context.getContentResolver(), uri);
    if (Log.isLoggable(TAG, Log.VERBOSE)) {
      String mimeType = context.getContentResolver().getType(uri);
      Log.v(
          TAG, "decoding " + uri + ", mimeType: " + mimeType + ", [" + width + ", " + height + "]");
    }
    return wrapped.decode(source, width, height, options);
  }
}
