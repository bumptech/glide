package com.bumptech.glide.load.model;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.resource.drawable.ResourceDrawableDecoder;
import com.bumptech.glide.signature.ObjectKey;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * Loads themed resource ids using {@link Resources#openRawResource(int)} or {@link
 * Resources#openRawResourceFd(int)} using the theme from {@link ResourceDrawableDecoder#THEME} when
 * it's available.
 *
 * @param <DataT> The type of data this {@code ModelLoader} will produce (e.g. {@link InputStream},
 *     {@link AssetFileDescriptor} etc).
 */
public final class DirectResourceLoader<DataT extends Closeable>
    implements ModelLoader<Integer, DataT> {

  private final Context context;
  private final ResourceOpener<DataT> resourceOpener;

  public static ModelLoaderFactory<Integer, InputStream> inputStreamFactory(Context context) {
    return new InputStreamFactory(context);
  }

  public static ModelLoaderFactory<Integer, AssetFileDescriptor> assetFileDescriptorFactory(
      Context context) {
    return new AssetFileDescriptorFactory(context);
  }

  DirectResourceLoader(Context context, ResourceOpener<DataT> resourceOpener) {
    this.context = context.getApplicationContext();
    this.resourceOpener = resourceOpener;
  }

  @Override
  public LoadData<DataT> buildLoadData(
      @NonNull Integer resourceId, int width, int height, @NonNull Options options) {
    Theme theme = options.get(ResourceDrawableDecoder.THEME);
    Resources resources =
        Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP && theme != null
            ? theme.getResources()
            : context.getResources();
    return new LoadData<>(
        // TODO(judds): We try to apply AndroidResourceSignature for caching in RequestBuilder.
        //  Arguably we should mix in that information here instead.
        new ObjectKey(resourceId),
        new ResourceDataFetcher<>(resources, resourceOpener, resourceId));
  }

  @Override
  public boolean handles(@NonNull Integer integer) {
    // We could check that this is in fact a resource ID, but doing so isn't free and in practice
    // it doesn't seem to have been an issue historically.
    return true;
  }

  private interface ResourceOpener<DataT> {
    DataT open(Resources resources, int resourceId);

    Class<DataT> getDataClass();
  }

  private static final class AssetFileDescriptorFactory
      implements ModelLoaderFactory<Integer, AssetFileDescriptor>,
          ResourceOpener<AssetFileDescriptor> {

    private final Context context;

    AssetFileDescriptorFactory(Context context) {
      this.context = context;
    }

    @Override
    public AssetFileDescriptor open(Resources resources, int resourceId) {
      return resources.openRawResourceFd(resourceId);
    }

    @Override
    public Class<AssetFileDescriptor> getDataClass() {
      return AssetFileDescriptor.class;
    }

    @NonNull
    @Override
    public ModelLoader<Integer, AssetFileDescriptor> build(
        @NonNull MultiModelLoaderFactory multiFactory) {
      return new DirectResourceLoader<>(context, this);
    }

    @Override
    public void teardown() {}
  }

  private static final class InputStreamFactory
      implements ModelLoaderFactory<Integer, InputStream>, ResourceOpener<InputStream> {

    private final Context context;

    InputStreamFactory(Context context) {
      this.context = context;
    }

    @NonNull
    @Override
    public ModelLoader<Integer, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
      return new DirectResourceLoader<>(context, this);
    }

    @Override
    public InputStream open(Resources resources, int resourceId) {
      return resources.openRawResource(resourceId);
    }

    @Override
    public Class<InputStream> getDataClass() {
      return InputStream.class;
    }

    @Override
    public void teardown() {}
  }

  private static final class ResourceDataFetcher<DataT extends Closeable>
      implements DataFetcher<DataT> {

    private final Resources resources;
    private final ResourceOpener<DataT> resourceOpener;
    private final int resourceId;
    private @Nullable DataT data;

    ResourceDataFetcher(Resources resources, ResourceOpener<DataT> resourceOpener, int resourceId) {
      this.resources = resources;
      this.resourceOpener = resourceOpener;
      this.resourceId = resourceId;
    }

    @Override
    public void loadData(
        @NonNull Priority priority, @NonNull DataCallback<? super DataT> callback) {
      try {
        data = resourceOpener.open(resources, resourceId);
        callback.onDataReady(data);
      } catch (Resources.NotFoundException e) {
        callback.onLoadFailed(e);
      }
    }

    @Override
    public void cleanup() {
      DataT local = data;
      if (local != null) {
        try {
          local.close();
        } catch (IOException e) {
          // Ignored.
        }
      }
    }

    @Override
    public void cancel() {}

    @NonNull
    @Override
    public Class<DataT> getDataClass() {
      return resourceOpener.getDataClass();
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
      return DataSource.LOCAL;
    }
  }
}
