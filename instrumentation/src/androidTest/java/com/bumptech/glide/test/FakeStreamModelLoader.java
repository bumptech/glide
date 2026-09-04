package com.bumptech.glide.test;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.data.DataFetcher.DataCallback;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoader.LoadData;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.signature.ObjectKey;
import com.google.common.io.ByteStreams;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public class FakeStreamModelLoader<T>
    implements ModelLoader<T, InputStream>, ModelLoaderFactory<T, InputStream> {

  private final Context context;
  private final int resourceId;

  public static byte[] getBytes(Context context, int resourceId) {
    try (InputStream is = context.getResources().openRawResource(resourceId)) {
      return ByteStreams.toByteArray(is);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public FakeStreamModelLoader(Context context, int resourceId) {
    this.context = context;
    this.resourceId = resourceId;
  }

  @Nullable
  @Override
  public LoadData<InputStream> buildLoadData(
      @NonNull Object o, int width, int height, @NonNull Options options) {
    return new LoadData<>(new ObjectKey(o), new Fetcher());
  }

  @Override
  public boolean handles(@NonNull Object o) {
    return true;
  }

  @NonNull
  @Override
  public ModelLoader<T, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
    return this;
  }

  @Override
  public void teardown() {}

  private final class Fetcher implements DataFetcher<InputStream> {
    private InputStream inputStream;

    @Override
    public void loadData(
        @NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
      inputStream = getInputStreamForResource(context, resourceId);
      callback.onDataReady(inputStream);
    }

    private InputStream getInputStreamForResource(Context context, @DrawableRes int resourceId) {
      Resources resources = context.getResources();
      try {
        Uri parse =
            Uri.parse(
                String.format(
                    Locale.US,
                    "%s://%s/%s/%s",
                    ContentResolver.SCHEME_ANDROID_RESOURCE,
                    resources.getResourcePackageName(resourceId),
                    resources.getResourceTypeName(resourceId),
                    resources.getResourceEntryName(resourceId)));
        return context.getContentResolver().openInputStream(parse);
      } catch (Resources.NotFoundException | FileNotFoundException e) {
        throw new IllegalArgumentException("Resource ID " + resourceId + " not found", e);
      }
    }

    @Override
    public void cleanup() {
      InputStream local = inputStream;
      if (local != null) {
        try {
          local.close();
        } catch (IOException e) {
          // Ignored.
        }
      }
    }

    @Override
    public void cancel() {
      // Do nothing.
    }

    @NonNull
    @Override
    public Class<InputStream> getDataClass() {
      return InputStream.class;
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
      return DataSource.LOCAL;
    }
  }
}
