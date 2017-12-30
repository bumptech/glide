package com.bumptech.glide.load.resource.drawable;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import java.util.List;

/**
 * Decodes {@link Drawable}s given resource {@link Uri}s.
 *
 * <p>This is typically used as a fallback for resource types that either aren't Bitmaps (see #350)
 * or for resource types that we can't obtain an {@link java.io.InputStream} for using a standard
 * {@link ContentResolver}, including some types of application icons and resources loaded from
 * other packages.
 */
public class ResourceDrawableDecoder implements ResourceDecoder<Uri, Drawable> {
  // android.resource://<package_name>/<type>/<name>.
  private static final int NAME_URI_PATH_SEGMENTS = 2;
  private static final int TYPE_PATH_SEGMENT_INDEX = 0;
  private static final int NAME_PATH_SEGMENT_INDEX = 1;
  // android.resource://<package_name>/<resource_id>
  private static final int ID_PATH_SEGMENTS = 1;
  private static final int RESOURCE_ID_SEGMENT_INDEX = 0;

  private final Context context;

  public ResourceDrawableDecoder(Context context) {
    this.context = context.getApplicationContext();
  }

  @Override
  public boolean handles(@NonNull Uri source, @NonNull Options options) {
    return source.getScheme().equals(ContentResolver.SCHEME_ANDROID_RESOURCE);
  }

  @Nullable
  @Override
  public Resource<Drawable> decode(@NonNull Uri source, int width, int height,
      @NonNull Options options) {
    @DrawableRes int resId = loadResourceIdFromUri(source);
    String packageName = source.getAuthority();
    Context toUse = packageName.equals(context.getPackageName())
        ? context : getContextForPackage(source, packageName);
    // We can't get a theme from another application.
    Drawable drawable = DrawableDecoderCompat.getDrawable(toUse, resId);
    return NonOwnedDrawableResource.newInstance(drawable);
  }

  @NonNull
  private Context getContextForPackage(Uri source, String packageName) {
    try {
      return context.createPackageContext(packageName, /*flags=*/ 0);
    } catch (NameNotFoundException e) {
      throw new IllegalArgumentException(
          "Failed to obtain context or unrecognized Uri format for: " + source, e);
    }
  }

  @DrawableRes
  private int loadResourceIdFromUri(Uri source) {
    List<String> segments = source.getPathSegments();
    @DrawableRes Integer result = null;
    if (segments.size() == NAME_URI_PATH_SEGMENTS) {
      String packageName = source.getAuthority();
      String typeName = segments.get(TYPE_PATH_SEGMENT_INDEX);
      String resourceName = segments.get(NAME_PATH_SEGMENT_INDEX);
      result = context.getResources().getIdentifier(resourceName, typeName, packageName);
    } else if (segments.size() == ID_PATH_SEGMENTS) {
      try {
        result = Integer.valueOf(segments.get(RESOURCE_ID_SEGMENT_INDEX));
      } catch (NumberFormatException e) {
        // Ignored.
      }
    }

    if (result == null) {
      throw new IllegalArgumentException("Unrecognized Uri format: " + source);
    } else if (result == 0) {
      throw new IllegalArgumentException("Failed to obtain resource id for: " + source);
    }
    return result;
  }
}
