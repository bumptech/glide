package com.bumptech.glide.load.model;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.resource.drawable.DrawableDecoderCompat;
import com.bumptech.glide.load.resource.drawable.ResourceDrawableDecoder;
import com.bumptech.glide.signature.ObjectKey;
import java.io.IOException;
import java.io.InputStream;

/**
 * Loads themed resource ids using {@link Resources#openRawResource(int)} or {@link
 * Resources#openRawResourceFd(int)} using the theme from {@link ResourceDrawableDecoder#THEME} when
 * it's available.
 *
 * <p>Resource ids from other packages are handled by {@link ResourceLoader} via {@link
 * ResourceDrawableDecoder} and {@link
 * com.bumptech.glide.load.resource.bitmap.ResourceBitmapDecoder}.
 *
 * @param <DataT> The type of data this {@code ModelLoader} will produce (e.g. {@link InputStream},
 *     {@link AssetFileDescriptor} etc).
 */
public final class DirectResourceLoader<DataT> implements ModelLoader<Integer, DataT> {

  private final Context context;
  private final ResourceOpener<DataT> resourceOpener;

  public static ModelLoaderFactory<Integer, InputStream> inputStreamFactory(Context context) {
    return new InputStreamFactory(context);
  }

  public static ModelLoaderFactory<Integer, AssetFileDescriptor> assetFileDescriptorFactory(
      Context context) {
    return new AssetFileDescriptorFactory(context);
  }

  public static ModelLoaderFactory<Integer, Drawable> drawableFactory(Context context) {
    return new DrawableFactory(context);
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
        new ResourceDataFetcher<>(theme, resources, resourceOpener, resourceId));
  }

  @Override
  public boolean handles(@NonNull Integer integer) {
    // We could check that this is in fact a resource ID, but doing so isn't free and in practice
    // it doesn't seem to have been an issue historically.
    return true;
  }

  private interface ResourceOpener<DataT> {

    /**
     * {@code resources} is expected to come from the given {@code theme}, so {@code theme} does not
     * need to be used if it's not required.
     */
    DataT open(@Nullable Theme theme, Resources resources, int resourceId);

    void close(DataT data) throws IOException;

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
    public AssetFileDescriptor open(@Nullable Theme theme, Resources resources, int resourceId) {
      return resources.openRawResourceFd(resourceId);
    }

    @Override
    public void close(AssetFileDescriptor data) throws IOException {
      data.close();
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
    public InputStream open(@Nullable Theme theme, Resources resources, int resourceId) {
      return resources.openRawResource(resourceId);
    }

    @Override
    public void close(InputStream data) throws IOException {
      data.close();
    }

    @Override
    public Class<InputStream> getDataClass() {
      return InputStream.class;
    }

    @Override
    public void teardown() {}
  }

  /**
   * Handles vectors, shapes and other resources that cannot be opened with
   * Resources.openRawResource. Overlaps in functionality with {@link ResourceDrawableDecoder} and
   * {@link com.bumptech.glide.load.resource.bitmap.ResourceBitmapDecoder} but it's more efficient
   * for simple resource loads within a single application.
   */
  private static final class DrawableFactory
      implements ModelLoaderFactory<Integer, Drawable>, ResourceOpener<Drawable> {

    private final Context context;

    DrawableFactory(Context context) {
      this.context = context;
    }

    @Override
    public Drawable open(@Nullable Theme theme, Resources resources, int resourceId) {
      // The Resources already includes the theme provided with the request, so we don't need to
      // provide the theme separately.
      return DrawableDecoderCompat.getDrawable(context, resourceId, theme);
    }

    @Override
    public void close(Drawable data) throws IOException {}

    @Override
    public Class<Drawable> getDataClass() {
      return Drawable.class;
    }

    @NonNull
    @Override
    public ModelLoader<Integer, Drawable> build(@NonNull MultiModelLoaderFactory multiFactory) {
      return new DirectResourceLoader<>(context, this);
    }

    @Override
    public void teardown() {}
  }

  private static final class ResourceDataFetcher<DataT> implements DataFetcher<DataT> {

    @Nullable private final Theme theme;
    private final Resources resources;
    private final ResourceOpener<DataT> resourceOpener;
    private final int resourceId;
    @Nullable private DataT data;

    ResourceDataFetcher(
        @Nullable Theme theme,
        Resources resources,
        ResourceOpener<DataT> resourceOpener,
        int resourceId) {
      this.theme = theme;
      this.resources = resources;
      this.resourceOpener = resourceOpener;
      this.resourceId = resourceId;
    }

    @Override
    public void loadData(
        @NonNull Priority priority, @NonNull DataCallback<? super DataT> callback) {
      try {
        data = resourceOpener.open(theme, resources, resourceId);
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
          resourceOpener.close(local);
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
