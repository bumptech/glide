package com.bumptech.glide.load.model;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.load.Options;
import java.io.InputStream;
import java.util.List;

/**
 * Converts Resource Uris to resource ids if the resource Uri points to a resource in this package.
 *
 * <p>This class works by parsing Uris into resource ids, then delegating the resource ID load to
 * other {@link ModelLoader}s, typically {@link DirectResourceLoader}.
 *
 * <p>This class really shouldn't need to exist. If you need to load resources, just pass in the
 * integer resource id directly using {@link com.bumptech.glide.RequestManager#load(Integer)}
 * instead. It'll be more correct in terms of caching and more efficient to load. The only reason
 * we're supporting this case is for backwards compatibility.
 *
 * <p>Because this class explicitly only handles resource Uris that are from the application's
 * package, resource uris from other packages are handled by {@link UriLoader}. {@link UriLoader} is
 * even less preferred because it can only handle certain resources from raw resources and it will
 * not apply appropriate theming, RTL or night mode attributes.
 *
 * @param <DataT> The type of data produced, e.g. {@link InputStream} or {@link
 *     AssetFileDescriptor}.
 */
public final class ResourceUriLoader<DataT> implements ModelLoader<Uri, DataT> {
  /**
   * See the javadoc on {@link android.content.res.Resources#getIdentifier(java.lang.String,
   * java.lang.String, java.lang.String)}.
   */
  private static final int INVALID_RESOURCE_ID = 0;

  private static final String TAG = "ResourceUriLoader";

  private final Context context;
  private final ModelLoader<Integer, DataT> delegate;

  public static ModelLoaderFactory<Uri, InputStream> newStreamFactory(Context context) {
    return new InputStreamFactory(context);
  }

  public static ModelLoaderFactory<Uri, AssetFileDescriptor> newAssetFileDescriptorFactory(
      Context context) {
    return new AssetFileDescriptorFactory(context);
  }

  ResourceUriLoader(Context context, ModelLoader<Integer, DataT> delegate) {
    this.context = context.getApplicationContext();
    this.delegate = delegate;
  }

  @Nullable
  @Override
  public LoadData<DataT> buildLoadData(
      @NonNull Uri uri, int width, int height, @NonNull Options options) {
    List<String> pathSegments = uri.getPathSegments();
    // android.resource//<package_name>/<resource_id>
    if (pathSegments.size() == 1) {
      return parseResourceIdUri(uri, width, height, options);
    }
    // android.resource//<package_name>/<drawable>/<resource_name>
    if (pathSegments.size() == 2) {
      return parseResourceNameUri(uri, width, height, options);
    }
    if (Log.isLoggable(TAG, Log.WARN)) {
      Log.w(TAG, "Failed to parse resource uri: " + uri);
    }
    return null;
  }

  @Nullable
  private LoadData<DataT> parseResourceNameUri(
      @NonNull Uri uri, int width, int height, @NonNull Options options) {
    List<String> pathSegments = uri.getPathSegments();
    String resourceType = pathSegments.get(0);
    String resourceName = pathSegments.get(1);

    // Yes it's bad, but the caller has chosen to give us a resource uri...
    @SuppressLint("DiscouragedApi")
    int identifier =
        context.getResources().getIdentifier(resourceName, resourceType, context.getPackageName());
    if (identifier == INVALID_RESOURCE_ID) {
      if (Log.isLoggable(TAG, Log.WARN)) {
        Log.w(TAG, "Failed to find resource id for: " + uri);
      }
      return null;
    }

    return delegate.buildLoadData(identifier, width, height, options);
  }

  @Nullable
  private LoadData<DataT> parseResourceIdUri(
      @NonNull Uri uri, int width, int height, @NonNull Options options) {
    try {
      int resourceId = Integer.parseInt(uri.getPathSegments().get(0));
      if (resourceId == INVALID_RESOURCE_ID) {
        if (Log.isLoggable(TAG, Log.WARN)) {
          Log.w(TAG, "Failed to parse a valid non-0 resource id from: " + uri);
        }
        return null;
      }
      return delegate.buildLoadData(resourceId, width, height, options);
    } catch (NumberFormatException e) {
      if (Log.isLoggable(TAG, Log.WARN)) {
        Log.w(TAG, "Failed to parse resource id from: " + uri, e);
      }
    }
    return null;
  }

  @Override
  public boolean handles(@NonNull Uri uri) {
    return ContentResolver.SCHEME_ANDROID_RESOURCE.equals(uri.getScheme())
        && context.getPackageName().equals(uri.getAuthority());
  }

  private static final class InputStreamFactory implements ModelLoaderFactory<Uri, InputStream> {

    private final Context context;

    InputStreamFactory(Context context) {
      this.context = context;
    }

    @NonNull
    @Override
    public ModelLoader<Uri, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
      return new ResourceUriLoader<>(context, multiFactory.build(Integer.class, InputStream.class));
    }

    @Override
    public void teardown() {}
  }

  private static final class AssetFileDescriptorFactory
      implements ModelLoaderFactory<Uri, AssetFileDescriptor> {

    private final Context context;

    AssetFileDescriptorFactory(Context context) {
      this.context = context;
    }

    @NonNull
    @Override
    public ModelLoader<Uri, AssetFileDescriptor> build(
        @NonNull MultiModelLoaderFactory multiFactory) {
      return new ResourceUriLoader<>(
          context, multiFactory.build(Integer.class, AssetFileDescriptor.class));
    }

    @Override
    public void teardown() {}
  }
}
