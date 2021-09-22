package com.bumptech.glide.load.model;

import android.content.ContentResolver;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import androidx.annotation.NonNull;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.data.FileDescriptorAssetPathFetcher;
import com.bumptech.glide.load.data.StreamAssetPathFetcher;
import com.bumptech.glide.signature.ObjectKey;
import java.io.InputStream;

/**
 * Loads a specific data type from an Asset Manager Uri.
 *
 * @param <Data> The type of data this loader will obtain.
 */
public class AssetUriLoader<Data> implements ModelLoader<Uri, Data> {
  private static final String ASSET_PATH_SEGMENT = "android_asset";
  private static final String ASSET_PREFIX =
      ContentResolver.SCHEME_FILE + ":///" + ASSET_PATH_SEGMENT + "/";
  private static final int ASSET_PREFIX_LENGTH = ASSET_PREFIX.length();

  private final AssetManager assetManager;
  private final AssetFetcherFactory<Data> factory;

  // Public API.
  @SuppressWarnings("WeakerAccess")
  public AssetUriLoader(AssetManager assetManager, AssetFetcherFactory<Data> factory) {
    this.assetManager = assetManager;
    this.factory = factory;
  }

  @Override
  public LoadData<Data> buildLoadData(
      @NonNull Uri model, int width, int height, @NonNull Options options) {
    String assetPath = model.toString().substring(ASSET_PREFIX_LENGTH);
    return new LoadData<>(new ObjectKey(model), factory.buildFetcher(assetManager, assetPath));
  }

  @Override
  public boolean handles(@NonNull Uri model) {
    return ContentResolver.SCHEME_FILE.equals(model.getScheme())
        && !model.getPathSegments().isEmpty()
        && ASSET_PATH_SEGMENT.equals(model.getPathSegments().get(0));
  }

  /**
   * A factory to build a {@link DataFetcher} for a specific asset path.
   *
   * @param <Data> The type of data that will be obtained by the fetcher.
   */
  public interface AssetFetcherFactory<Data> {
    DataFetcher<Data> buildFetcher(AssetManager assetManager, String assetPath);
  }

  /** Factory for loading {@link InputStream}s from asset manager Uris. */
  public static class StreamFactory
      implements ModelLoaderFactory<Uri, InputStream>, AssetFetcherFactory<InputStream> {

    private final AssetManager assetManager;

    public StreamFactory(AssetManager assetManager) {
      this.assetManager = assetManager;
    }

    @NonNull
    @Override
    public ModelLoader<Uri, InputStream> build(MultiModelLoaderFactory multiFactory) {
      return new AssetUriLoader<>(assetManager, this);
    }

    @Override
    public void teardown() {
      // Do nothing.
    }

    @Override
    public DataFetcher<InputStream> buildFetcher(AssetManager assetManager, String assetPath) {
      return new StreamAssetPathFetcher(assetManager, assetPath);
    }
  }

  /** Factory for loading {@link ParcelFileDescriptor}s from asset manager Uris. */
  public static class FileDescriptorFactory
      implements ModelLoaderFactory<Uri, ParcelFileDescriptor>,
          AssetFetcherFactory<ParcelFileDescriptor> {

    private final AssetManager assetManager;

    public FileDescriptorFactory(AssetManager assetManager) {
      this.assetManager = assetManager;
    }

    @NonNull
    @Override
    public ModelLoader<Uri, ParcelFileDescriptor> build(MultiModelLoaderFactory multiFactory) {
      return new AssetUriLoader<>(assetManager, this);
    }

    @Override
    public void teardown() {
      // Do nothing.
    }

    @Override
    public DataFetcher<ParcelFileDescriptor> buildFetcher(
        AssetManager assetManager, String assetPath) {
      return new FileDescriptorAssetPathFetcher(assetManager, assetPath);
    }
  }
}
