package com.bumptech.glide.load.model;

import android.content.ContentResolver;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import androidx.annotation.NonNull;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.AssetFileDescriptorLocalUriFetcher;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.data.FileDescriptorLocalUriFetcher;
import com.bumptech.glide.load.data.StreamLocalUriFetcher;
import com.bumptech.glide.signature.ObjectKey;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A ModelLoader for {@link android.net.Uri}s that handles local {@link android.net.Uri}s directly
 * and routes remote {@link android.net.Uri}s to a wrapped {@link
 * com.bumptech.glide.load.model.ModelLoader} that handles {@link
 * com.bumptech.glide.load.model.GlideUrl}s.
 *
 * @param <Data> The type of data that will be retrieved for {@link android.net.Uri}s.
 */
public class UriLoader<Data> implements ModelLoader<Uri, Data> {
  private static final Set<String> SCHEMES =
      Collections.unmodifiableSet(
          new HashSet<>(
              Arrays.asList(
                  ContentResolver.SCHEME_FILE,
                  ContentResolver.SCHEME_ANDROID_RESOURCE,
                  ContentResolver.SCHEME_CONTENT)));

  private final LocalUriFetcherFactory<Data> factory;

  // Public API.
  @SuppressWarnings("WeakerAccess")
  public UriLoader(LocalUriFetcherFactory<Data> factory) {
    this.factory = factory;
  }

  @Override
  public LoadData<Data> buildLoadData(
      @NonNull Uri model, int width, int height, @NonNull Options options) {
    return new LoadData<>(new ObjectKey(model), factory.build(model));
  }

  @Override
  public boolean handles(@NonNull Uri model) {
    return SCHEMES.contains(model.getScheme());
  }

  /**
   * Factory for obtaining a {@link DataFetcher} for a data type for a particular {@link Uri}.
   *
   * @param <Data> The type of data the returned {@link DataFetcher} will obtain.
   */
  public interface LocalUriFetcherFactory<Data> {
    DataFetcher<Data> build(Uri uri);
  }

  /** Loads {@link InputStream}s from {@link Uri}s. */
  public static class StreamFactory
      implements ModelLoaderFactory<Uri, InputStream>, LocalUriFetcherFactory<InputStream> {

    private final ContentResolver contentResolver;

    public StreamFactory(ContentResolver contentResolver) {
      this.contentResolver = contentResolver;
    }

    @Override
    public DataFetcher<InputStream> build(Uri uri) {
      return new StreamLocalUriFetcher(contentResolver, uri);
    }

    @NonNull
    @Override
    public ModelLoader<Uri, InputStream> build(MultiModelLoaderFactory multiFactory) {
      return new UriLoader<>(this);
    }

    @Override
    public void teardown() {
      // Do nothing.
    }
  }

  /** Loads {@link ParcelFileDescriptor}s from {@link Uri}s. */
  public static class FileDescriptorFactory
      implements ModelLoaderFactory<Uri, ParcelFileDescriptor>,
          LocalUriFetcherFactory<ParcelFileDescriptor> {

    private final ContentResolver contentResolver;

    public FileDescriptorFactory(ContentResolver contentResolver) {
      this.contentResolver = contentResolver;
    }

    @Override
    public DataFetcher<ParcelFileDescriptor> build(Uri uri) {
      return new FileDescriptorLocalUriFetcher(contentResolver, uri);
    }

    @NonNull
    @Override
    public ModelLoader<Uri, ParcelFileDescriptor> build(MultiModelLoaderFactory multiFactory) {
      return new UriLoader<>(this);
    }

    @Override
    public void teardown() {
      // Do nothing.
    }
  }

  /** Loads {@link AssetFileDescriptor}s from {@link Uri}s. */
  public static final class AssetFileDescriptorFactory
      implements ModelLoaderFactory<Uri, AssetFileDescriptor>,
          LocalUriFetcherFactory<AssetFileDescriptor> {

    private final ContentResolver contentResolver;

    public AssetFileDescriptorFactory(ContentResolver contentResolver) {
      this.contentResolver = contentResolver;
    }

    @Override
    public ModelLoader<Uri, AssetFileDescriptor> build(MultiModelLoaderFactory multiFactory) {
      return new UriLoader<>(this);
    }

    @Override
    public void teardown() {
      // Do nothing.
    }

    @Override
    public DataFetcher<AssetFileDescriptor> build(Uri uri) {
      return new AssetFileDescriptorLocalUriFetcher(contentResolver, uri);
    }
  }
}
