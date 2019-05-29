package com.bumptech.glide.test;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.InstrumentationRegistry;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.test.WaitModelLoader.WaitModel;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

/**
 * Allows callers to load an object but force the load to pause until {@link WaitModel#countDown()}
 * is called.
 */
public final class WaitModelLoader<Model, Data> implements ModelLoader<WaitModel<Model>, Data> {

  private final ModelLoader<Model, Data> wrapped;

  private WaitModelLoader(ModelLoader<Model, Data> wrapped) {
    this.wrapped = wrapped;
  }

  @Nullable
  @Override
  public LoadData<Data> buildLoadData(
      @NonNull WaitModel<Model> waitModel, int width, int height, @NonNull Options options) {
    LoadData<Data> wrappedLoadData =
        wrapped.buildLoadData(waitModel.wrapped, width, height, options);
    if (wrappedLoadData == null) {
      return null;
    }
    return new LoadData<>(
        wrappedLoadData.sourceKey, new WaitFetcher<>(wrappedLoadData.fetcher, waitModel.latch));
  }

  @Override
  public boolean handles(@NonNull WaitModel<Model> waitModel) {
    return wrapped.handles(waitModel.wrapped);
  }

  public static final class WaitModel<T> {
    private final CountDownLatch latch = new CountDownLatch(1);
    private final T wrapped;

    WaitModel(T wrapped) {
      this.wrapped = wrapped;
    }

    public void countDown() {
      if (latch.getCount() != 1) {
        throw new IllegalStateException();
      }
      latch.countDown();
    }
  }

  public static final class Factory<Model, Data>
      implements ModelLoaderFactory<WaitModel<Model>, Data> {

    private final Class<Model> modelClass;
    private final Class<Data> dataClass;

    Factory(Class<Model> modelClass, Class<Data> dataClass) {
      this.modelClass = modelClass;
      this.dataClass = dataClass;
    }

    public static synchronized <T> WaitModel<T> waitOn(T model) {
      @SuppressWarnings("unchecked")
      ModelLoaderFactory<WaitModel<T>, InputStream> streamFactory =
          new Factory<>((Class<T>) model.getClass(), InputStream.class);
      Glide.get(InstrumentationRegistry.getTargetContext())
          .getRegistry()
          .replace(WaitModel.class, InputStream.class, streamFactory);

      return new WaitModel<>(model);
    }

    @NonNull
    @Override
    public ModelLoader<WaitModel<Model>, Data> build(MultiModelLoaderFactory multiFactory) {
      return new WaitModelLoader<>(multiFactory.build(modelClass, dataClass));
    }

    @Override
    public void teardown() {
      // Do nothing.
    }
  }

  private static final class WaitFetcher<Data> implements DataFetcher<Data> {

    private final DataFetcher<Data> wrapped;
    private final CountDownLatch toWaitOn;

    WaitFetcher(DataFetcher<Data> wrapped, CountDownLatch toWaitOn) {
      this.wrapped = wrapped;
      this.toWaitOn = toWaitOn;
    }

    @Override
    public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super Data> callback) {
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
    public Class<Data> getDataClass() {
      return wrapped.getDataClass();
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
      return wrapped.getDataSource();
    }
  }
}
