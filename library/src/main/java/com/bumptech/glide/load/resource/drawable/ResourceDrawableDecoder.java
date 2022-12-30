package com.bumptech.glide.load.resource.drawable;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.util.Preconditions;
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

  /** Specifies a {@link Theme} which will be used to load the drawable. */
  public static final Option<Theme> THEME =
      Option.memory("com.bumptech.glide.load.resource.bitmap.Downsampler.Theme");

  /**
   * The package name to provide {@link Resources#getIdentifier(String, String, String)} when trying
   * to find system resource ids.
   *
   * <p>As far as I can tell this is undocumented, but works.
   */
  private static final String ANDROID_PACKAGE_NAME = "android";
  /**
   * {@link Resources#getIdentifier(String, String, String)} documents that it will return 0 and
   * that 0 is not a valid resouce id.
   */
  private static final int MISSING_RESOURCE_ID = 0;
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
    String scheme = source.getScheme();
    return scheme != null && scheme.equals(ContentResolver.SCHEME_ANDROID_RESOURCE);
  }

  @Nullable
  @Override
  public Resource<Drawable> decode(
      @NonNull Uri source, int width, int height, @NonNull Options options) {
    String packageName = source.getAuthority();
    if (TextUtils.isEmpty(packageName)) {
      throw new IllegalStateException("Package name for " + source + " is null or empty");
    }
    Context targetContext = findContextForPackage(source, packageName);
    @DrawableRes int resId = findResourceIdFromUri(targetContext, source);
    // Only use the provided theme if we're loading resources from our package. We can't get themes
    // from other packages and we don't want to use a theme from our package when loading another
    // package's resources.
    Theme theme =
        Preconditions.checkNotNull(packageName).equals(context.getPackageName())
            ? options.get(THEME)
            : null;
    Drawable drawable =
        theme == null
            ? DrawableDecoderCompat.getDrawable(context, targetContext, resId)
            : DrawableDecoderCompat.getDrawable(context, resId, theme);
    return NonOwnedDrawableResource.newInstance(drawable);
  }

  @NonNull
  private Context findContextForPackage(Uri source, @NonNull String packageName) {
    // Fast path
    if (packageName.equals(context.getPackageName())) {
      return context;
    }

    try {
      return context.createPackageContext(packageName, /* flags= */ 0);
    } catch (NameNotFoundException e) {
      // The parent APK holds the correct context if the resource is located in a split
      if (packageName.contains(context.getPackageName())) {
        return context;
      }

      throw new IllegalArgumentException(
          "Failed to obtain context or unrecognized Uri format for: " + source, e);
    }
  }

  @DrawableRes
  private int findResourceIdFromUri(Context context, Uri source) {
    List<String> segments = source.getPathSegments();
    if (segments.size() == NAME_URI_PATH_SEGMENTS) {
      return findResourceIdFromTypeAndNameResourceUri(context, source);
    } else if (segments.size() == ID_PATH_SEGMENTS) {
      return findResourceIdFromResourceIdUri(source);
    } else {
      throw new IllegalArgumentException("Unrecognized Uri format: " + source);
    }
  }

  // android.resource://com.android.camera2/mipmap/logo_camera_color
  @DrawableRes
  private int findResourceIdFromTypeAndNameResourceUri(Context context, Uri source) {
    List<String> segments = source.getPathSegments();
    String packageName = source.getAuthority();
    String typeName = segments.get(TYPE_PATH_SEGMENT_INDEX);
    String resourceName = segments.get(NAME_PATH_SEGMENT_INDEX);
    int result = context.getResources().getIdentifier(resourceName, typeName, packageName);
    if (result == MISSING_RESOURCE_ID) {
      result = Resources.getSystem().getIdentifier(resourceName, typeName, ANDROID_PACKAGE_NAME);
    }
    if (result == MISSING_RESOURCE_ID) {
      throw new IllegalArgumentException("Failed to find resource id for: " + source);
    }
    return result;
  }

  // android.resource://com.android.camera2/123456
  @DrawableRes
  private int findResourceIdFromResourceIdUri(Uri source) {
    List<String> segments = source.getPathSegments();
    try {
      return Integer.parseInt(segments.get(RESOURCE_ID_SEGMENT_INDEX));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Unrecognized Uri format: " + source, e);
    }
  }
}
