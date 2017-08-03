package com.bumptech.glide.load.model;

import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.text.TextUtils;
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

  public StringLoader(ModelLoader<Uri, Data> uriLoader) {
    this.uriLoader = uriLoader;
  }

  @Override
  public LoadData<Data> buildLoadData(String model, int width, int height,
      Options options) {
    Uri uri = parseUri(model);
    return uri == null ? null : uriLoader.buildLoadData(uri, width, height, options);
  }

  @Override
  public boolean handles(String model) {
    return true;
  }

  @Nullable
  private static Uri parseUri(String model) {
    Uri uri;
    if (TextUtils.isEmpty(model)) {
      return null;
    } else if (model.startsWith("/")) {
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

  /**
   * Factory for loading {@link InputStream}s from Strings.
   */
  public static class StreamFactory implements ModelLoaderFactory<String, InputStream> {

    @Override
    public ModelLoader<String, InputStream> build(MultiModelLoaderFactory multiFactory) {
      return new StringLoader<>(multiFactory.build(Uri.class, InputStream.class));
    }

    @Override
    public void teardown() {
      // Do nothing.
    }
  }

  /**
   * Factory for loading {@link ParcelFileDescriptor}s from Strings.
   */
  public static class FileDescriptorFactory
      implements ModelLoaderFactory<String, ParcelFileDescriptor> {

    @Override
    public ModelLoader<String, ParcelFileDescriptor> build(MultiModelLoaderFactory multiFactory) {
      return new StringLoader<>(multiFactory.build(Uri.class, ParcelFileDescriptor.class));
    }

    @Override
    public void teardown() {
      // Do nothing.
    }
  }
}
