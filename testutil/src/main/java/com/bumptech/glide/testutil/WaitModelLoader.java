package com.bumptech.glide.testutil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.testutil.WaitModelLoader.WaitModel;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

/**
 * Allows callers to load an object but force the load to pause until {@link WaitModel#countDown()}
 * is called.
 */
public final class WaitModelLoader<ModelT, DataT> implements ModelLoader<WaitModel<ModelT>, DataT> {

  /**
   * A Model that can be loaded with Glide where the load will be blocked from completing until
   * {@link #countDown()} is called.
   *
   * <p>This class allows us to test what Glide does while a load is in progress.
   */
  public static final class WaitModel<ModelT> {
    private final CountDownLatch latch = new CountDownLatch(1);
    private final ModelT wrapped;

    WaitModel(ModelT wrapped) {
      this.wrapped = wrapped;
    }

    public void countDown() {
      if (latch.getCount() != 1) {
        throw new IllegalStateException();
      }
      latch.countDown();
    }
  }

  /**
   * @deprecated Use {@link WaitModelLoaderRule#waitOn(Object)} instead
   */
  @Deprecated
  public static synchronized <T> WaitModel<T> waitOn(T model) {
    @SuppressWarnings("unchecked")
    ModelLoaderFactory<WaitModel<T>, InputStream> streamFactory =
        new Factory<>((Class<T>) model.getClass(), InputStream.class);
    Glide.get(ApplicationProvider.getApplicationContext())
        .getRegistry()
        .replace(WaitModel.class, InputStream.class, streamFactory);

    return new WaitModel<>(model);
  }

  private final ModelLoader<ModelT, DataT> wrapped;

  private WaitModelLoader(ModelLoader<ModelT, DataT> wrapped) {
    this.wrapped = wrapped;
  }

  @Nullable
  @Override
  public LoadData<DataT> buildLoadData(
      @NonNull WaitModel<ModelT> waitModel, int width, int height, @NonNull Options options) {
    LoadData<DataT> wrappedLoadData =
        wrapped.buildLoadData(waitModel.wrapped, width, height, options);
    if (wrappedLoadData == null) {
      return null;
    }
    return new LoadData<>(
        wrappedLoadData.sourceKey, new WaitFetcher<>(wrappedLoadData.fetcher, waitModel.latch));
  }

  @Override
  public boolean handles(@NonNull WaitModel<ModelT> waitModel) {
    return wrapped.handles(waitModel.wrapped);
  }

  private static final class Factory<ModelT, DataT>
      implements ModelLoaderFactory<WaitModel<ModelT>, DataT> {

    private final Class<ModelT> modelClass;
    private final Class<DataT> dataClass;

    Factory(Class<ModelT> modelClass, Class<DataT> dataClass) {
      this.modelClass = modelClass;
      this.dataClass = dataClass;
    }

    @NonNull
    @Override
    public ModelLoader<WaitModel<ModelT>, DataT> build(MultiModelLoaderFactory multiFactory) {
      return new WaitModelLoader<>(multiFactory.build(modelClass, dataClass));
    }

    @Override
    public void teardown() {
      // Do nothing.
    }
  }

  private static final class WaitFetcher<DataT> implements DataFetcher<DataT> {

    private final DataFetcher<DataT> wrapped;
    private final CountDownLatch toWaitOn;

    WaitFetcher(DataFetcher<DataT> wrapped, CountDownLatch toWaitOn) {
      this.wrapped = wrapped;
      this.toWaitOn = toWaitOn;
    }

    @Override
    public void loadData(
        @NonNull Priority priority, @NonNull DataCallback<? super DataT> callback) {
      ConcurrencyHelper.waitOnLatch(toWaitOn);
      wrapped.loadData(priority, callback);
    }

    @Override
    public void cleanup() {
      wrapped.cleanup();
    }

    @Override
    public void cancel() {
      wrapped.cancel();
    }

    @NonNull
    @Override
    public Class<DataT> getDataClass() {
      return wrapped.getDataClass();
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
      return wrapped.getDataSource();
    }
  }
}
