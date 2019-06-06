package com.bumptech.glide.load.model;

import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.load.Options;
import java.io.File;
import java.io.InputStream;

/**
 * A model loader for handling certain string models. Handles paths, urls, and any uri string with a
 * scheme handled by {@link android.content.ContentResolver#openInputStream(Uri)}.
 *
 * @param <Data> The type of data that will be loaded from the given {@link java.lang.String}.
 */
public class StringLoader<Data> implements ModelLoader<String, Data> {
  private final ModelLoader<Uri, Data> uriLoader;

  // Public API.
  @SuppressWarnings("WeakerAccess")
  public StringLoader(ModelLoader<Uri, Data> uriLoader) {
    this.uriLoader = uriLoader;
  }

  @Override
  public LoadData<Data> buildLoadData(
      @NonNull String model, int width, int height, @NonNull Options options) {
    Uri uri = parseUri(model);
    if (uri == null || !uriLoader.handles(uri)) {
      return null;
    }
    return uriLoader.buildLoadData(uri, width, height, options);
  }

  @Override
  public boolean handles(@NonNull String model) {
    // Avoid parsing the Uri twice and simply return null from buildLoadData if we don't handle this
    // particular Uri type.
    return true;
  }

  @Nullable
  private static Uri parseUri(String model) {
    Uri uri;
    if (TextUtils.isEmpty(model)) {
      return null;
      // See https://pmd.github.io/pmd-6.0.0/pmd_rules_java_performance.html#simplifystartswith
    } else if (model.charAt(0) == '/') {
      uri = toFileUri(model);
    } else {
      uri = Uri.parse(model);
      String scheme = uri.getScheme();
      if (scheme == null) {
        uri = toFileUri(model);
      }
    }
    return uri;
  }

  private static Uri toFileUri(String path) {
    return Uri.fromFile(new File(path));
  }

  /** Factory for loading {@link InputStream}s from Strings. */
  public static class StreamFactory implements ModelLoaderFactory<String, InputStream> {

    @NonNull
    @Override
    public ModelLoader<String, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
      return new StringLoader<>(multiFactory.build(Uri.class, InputStream.class));
    }

    @Override
    public void teardown() {
      // Do nothing.
    }
  }

  /** Factory for loading {@link ParcelFileDescriptor}s from Strings. */
  public static class FileDescriptorFactory
      implements ModelLoaderFactory<String, ParcelFileDescriptor> {

    @NonNull
    @Override
    public ModelLoader<String, ParcelFileDescriptor> build(
        @NonNull MultiModelLoaderFactory multiFactory) {
      return new StringLoader<>(multiFactory.build(Uri.class, ParcelFileDescriptor.class));
    }

    @Override
    public void teardown() {
      // Do nothing.
    }
  }

  /** Loads {@link AssetFileDescriptor}s from Strings. */
  public static final class AssetFileDescriptorFactory
      implements ModelLoaderFactory<String, AssetFileDescriptor> {

    @Override
    public ModelLoader<String, AssetFileDescriptor> build(
        @NonNull MultiModelLoaderFactory multiFactory) {
      return new StringLoader<>(multiFactory.build(Uri.class, AssetFileDescriptor.class));
    }

    @Override
    public void teardown() {
      // Do nothing.
    }
  }
}
