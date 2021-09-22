package com.bumptech.glide.load.model;

import android.content.ContentResolver;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.load.Options;
import java.io.InputStream;

/**
 * A model loader for handling Android resource files. Model must be an Android resource id in the
 * package of the given context.
 *
 * @param <Data> The type of data that will be loaded for the given android resource.
 */
public class ResourceLoader<Data> implements ModelLoader<Integer, Data> {
  private static final String TAG = "ResourceLoader";
  private final ModelLoader<Uri, Data> uriLoader;
  private final Resources resources;

  // Public API.
  @SuppressWarnings("WeakerAccess")
  public ResourceLoader(Resources resources, ModelLoader<Uri, Data> uriLoader) {
    this.resources = resources;
    this.uriLoader = uriLoader;
  }

  @Override
  public LoadData<Data> buildLoadData(
      @NonNull Integer model, int width, int height, @NonNull Options options) {
    Uri uri = getResourceUri(model);
    return uri == null ? null : uriLoader.buildLoadData(uri, width, height, options);
  }

  @Nullable
  private Uri getResourceUri(Integer model) {
    try {
      return Uri.parse(
          ContentResolver.SCHEME_ANDROID_RESOURCE
              + "://"
              + resources.getResourcePackageName(model)
              + '/'
              + resources.getResourceTypeName(model)
              + '/'
              + resources.getResourceEntryName(model));
    } catch (Resources.NotFoundException e) {
      if (Log.isLoggable(TAG, Log.WARN)) {
        Log.w(TAG, "Received invalid resource id: " + model, e);
      }
      return null;
    }
  }

  @Override
  public boolean handles(@NonNull Integer model) {
    // TODO: check that this is in fact a resource id.
    return true;
  }

  /** Factory for loading {@link InputStream}s from Android resource ids. */
  public static class StreamFactory implements ModelLoaderFactory<Integer, InputStream> {

    private final Resources resources;

    public StreamFactory(Resources resources) {
      this.resources = resources;
    }

    @NonNull
    @Override
    public ModelLoader<Integer, InputStream> build(MultiModelLoaderFactory multiFactory) {
      return new ResourceLoader<>(resources, multiFactory.build(Uri.class, InputStream.class));
    }

    @Override
    public void teardown() {
      // Do nothing.
    }
  }

  /** Factory for loading {@link ParcelFileDescriptor}s from Android resource ids. */
  public static class FileDescriptorFactory
      implements ModelLoaderFactory<Integer, ParcelFileDescriptor> {

    private final Resources resources;

    public FileDescriptorFactory(Resources resources) {
      this.resources = resources;
    }

    @NonNull
    @Override
    public ModelLoader<Integer, ParcelFileDescriptor> build(MultiModelLoaderFactory multiFactory) {
      return new ResourceLoader<>(
          resources, multiFactory.build(Uri.class, ParcelFileDescriptor.class));
    }

    @Override
    public void teardown() {
      // Do nothing.
    }
  }

  /** Loads {@link AssetFileDescriptor}s from resource ids. */
  public static final class AssetFileDescriptorFactory
      implements ModelLoaderFactory<Integer, AssetFileDescriptor> {

    private final Resources resources;

    public AssetFileDescriptorFactory(Resources resources) {
      this.resources = resources;
    }

    @Override
    public ModelLoader<Integer, AssetFileDescriptor> build(MultiModelLoaderFactory multiFactory) {
      return new ResourceLoader<>(
          resources, multiFactory.build(Uri.class, AssetFileDescriptor.class));
    }

    @Override
    public void teardown() {
      // Do nothing.
    }
  }

  /** Factory for loading resource {@link Uri}s from Android resource ids. */
  public static class UriFactory implements ModelLoaderFactory<Integer, Uri> {

    private final Resources resources;

    public UriFactory(Resources resources) {
      this.resources = resources;
    }

    @NonNull
    @Override
    public ModelLoader<Integer, Uri> build(MultiModelLoaderFactory multiFactory) {
      return new ResourceLoader<>(resources, UnitModelLoader.<Uri>getInstance());
    }

    @Override
    public void teardown() {
      // Do nothing.
    }
  }
}
