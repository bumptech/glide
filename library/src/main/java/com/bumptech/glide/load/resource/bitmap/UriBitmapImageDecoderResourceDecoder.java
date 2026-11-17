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

/**
 * Decodes {@link Bitmap}s from {@link Uri}s using {@link ImageDecoder} on Android 15+ (API 35+).
 *
 * <p>Direct Uri decoding using {@link ImageDecoder#createSource} is restricted to Android 15+
 * ({@link Build.VERSION_CODES#VANILLA_ICE_CREAM}) because earlier Android versions encounter native
 * container descriptor race conditions on multi-track containers (e.g. motion photos, gainmaps). On
 * earlier Android versions, Glide automatically falls back to input streams.
 */
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
public final class UriBitmapImageDecoderResourceDecoder implements ResourceDecoder<Uri, Bitmap> {
  private static final String TAG = "UriBitmapDecoder";
  private final Context context;
  private final BitmapImageDecoderResourceDecoder wrapped = new BitmapImageDecoderResourceDecoder();

  private final boolean onlyRejectPositiveVideoFrames;

  public UriBitmapImageDecoderResourceDecoder(@NonNull Context context) {
    this(context, /* onlyRejectPositiveVideoFrames= */ false);
  }

  /**
   * @param onlyRejectPositiveVideoFrames {@code true} to short-circuit {@link #handles} only for
   *     requests with a positive {@link VideoDecoder#TARGET_FRAME}, so that {@code frame(0)}
   *     requests fall through to the MIME type check. {@code false} short-circuits for any {@link
   *     VideoDecoder#TARGET_FRAME} other than {@link VideoDecoder#DEFAULT_FRAME}. See {@link
   *     com.bumptech.glide.GlideBuilder#setPrioritizeFileDescriptorOverInputStream(boolean)}.
   */
  public UriBitmapImageDecoderResourceDecoder(
      @NonNull Context context, boolean onlyRejectPositiveVideoFrames) {
    this.context = context.getApplicationContext();
    this.onlyRejectPositiveVideoFrames = onlyRejectPositiveVideoFrames;
  }

  @Override
  public boolean handles(@NonNull Uri uri, @NonNull Options options) throws IOException {
    if (onlyRejectPositiveVideoFrames) {
      // Some callers set frame(0) on base request options for all loads, so 0 and
      // DEFAULT_FRAME (-1) fall through to the MIME type check below, which excludes videos.
      if (ImageDecoderSupport.hasExplicitVideoFrame(options)) {
        return false;
      }
    } else {
      // Videos are excluded by the MIME type check below. This is only a shortcut for explicit
      // video frame requests (RequestOptions#frame), which are typically extension-less MediaStore
      // URIs and would otherwise pay a Binder IPC to the provider in getMimeType() before being
      // rejected. It cannot catch frame(DEFAULT_FRAME): Options#get returns DEFAULT_FRAME when the
      // option is unset, so such requests are indistinguishable from plain image requests and fall
      // through to the MIME type check.
      Long targetFrame = options.get(VideoDecoder.TARGET_FRAME);
      if (targetFrame != null && targetFrame != VideoDecoder.DEFAULT_FRAME) {
        return false;
      }
    }
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
    String lastSegment = uri.getLastPathSegment();
    if (lastSegment != null) {
      int lastDot = lastSegment.lastIndexOf('.');
      if (lastDot != -1) {
        String extension = lastSegment.substring(lastDot + 1);
        String mimeType =
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase(Locale.ROOT));
        if (mimeType != null) {
          return mimeType;
        }
      }
    }
    return context.getContentResolver().getType(uri);
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
