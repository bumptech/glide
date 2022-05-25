package com.bumptech.glide.testutil;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.data.DataFetcher.DataCallback;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.signature.ObjectKey;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

public final class MockModelLoader<ModelT, DataT> implements ModelLoader<ModelT, DataT> {
  private final ModelT model;
  private final Class<DataT> dataClass;
  private final ListenableFuture<DataT> dataFuture;

  @SuppressWarnings("unchecked")
  public static <ModelT, DataT> void mock(final ModelT model, final DataT data) {
    mockAsync(model, (Class<DataT>) data.getClass(), Futures.immediateFuture(data));
  }

  @SuppressWarnings("unchecked")
  public static <ModelT, DataT> void mockAsync(
      final ModelT model, final Class<DataT> dataClass, final ListenableFuture<DataT> dataFuture) {
    Context context = ApplicationProvider.getApplicationContext();

    Glide.get(context)
        .getRegistry()
        .replace(
            (Class<ModelT>) model.getClass(),
            dataClass,
            new ModelLoaderFactory<ModelT, DataT>() {
              @NonNull
              @Override
              public ModelLoader<ModelT, DataT> build(
                  @NonNull MultiModelLoaderFactory multiFactory) {
                return new MockModelLoader<>(model, dataClass, dataFuture);
              }

              @Override
              public void teardown() {
                // Do nothing.
              }
            });
  }

  private MockModelLoader(
      ModelT model, Class<DataT> dataClass, ListenableFuture<DataT> dataFuture) {
    this.model = model;
    this.dataClass = dataClass;
    this.dataFuture = dataFuture;
  }

  @Override
  public LoadData<DataT> buildLoadData(
      @NonNull ModelT modelT, int width, int height, @NonNull Options options) {
    return new LoadData<>(new ObjectKey(modelT), new MockDataFetcher<>(dataClass, dataFuture));
  }

  @Override
  public boolean handles(@NonNull ModelT model) {
    return this.model.equals(model);
  }

  private static final class MockDataFetcher<DataT> implements DataFetcher<DataT> {

    private final ListenableFuture<DataT> dataFuture;
    private final Class<DataT> dataClass;

    MockDataFetcher(Class<DataT> dataClass, ListenableFuture<DataT> dataFuture) {
      this.dataClass = dataClass;
      this.dataFuture = dataFuture;
    }

    @Override
    public void loadData(
        @NonNull Priority priority, final @NonNull DataCallback<? super DataT> callback) {
      Futures.addCallback(
          dataFuture,
          new FutureCallback<DataT>() {
            @Override
            public void onSuccess(DataT data) {
              callback.onDataReady(data);
            }

            @Override
            public void onFailure(Throwable t) {
              if (t instanceof Exception) {
                callback.onLoadFailed((Exception) t);
              } else {
                callback.onLoadFailed(new Exception(t));
              }
            }
          },
          MoreExecutors.directExecutor());
    }

    @Override
    public void cleanup() {
      // Do nothing.
    }

    @Override
    public void cancel() {
      dataFuture.cancel(true);
    }

    @NonNull
    @Override
    public Class<DataT> getDataClass() {
      return dataClass;
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
      return DataSource.REMOTE;
    }
  }
}
