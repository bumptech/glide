package com.bumptech.glide.load.resource.bitmap;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.ImageDecoder.Source;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import java.io.IOException;

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
    String mimeType = context.getContentResolver().getType(uri);
    // Skip GIFs to avoid decoding them as static Bitmaps. Glide otherwise prefers this direct path
    // over the indirect animation path when a Drawable is requested, which prevents GIFs from
    // animating.
    if (mimeType != null && mimeType.equals("image/gif")) {
      return false;
    }
    return true;
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
