package com.bumptech.glide.test;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;
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
 *
 * <p>
 */
public final class WaitModelLoader<Model, Data>
    implements ModelLoader<WaitModel<Model>, Data> {

  private final ModelLoader<Model, Data> wrapped;

  WaitModelLoader(ModelLoader<Model, Data> wrapped) {
    this.wrapped = wrapped;
  }

  @Nullable
  @Override
  public LoadData<Data> buildLoadData(
      WaitModel<Model> waitModel, int width, int height, Options options) {
    LoadData<Data> wrappedLoadData = wrapped
        .buildLoadData(waitModel.wrapped, width, height, options);
    if (wrappedLoadData == null) {
      return null;
    }
    return new LoadData<>(
        wrappedLoadData.sourceKey, new WaitFetcher<>(wrappedLoadData.fetcher, waitModel.latch));
  }

  @Override
  public boolean handles(WaitModel<Model> waitModel) {
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
      implements ModelLoaderFactory<WaitModel, Data> {

    private final Class<Model> modelClass;
    private final Class<Data> dataClass;

    Factory(Class<Model> modelClass, Class<Data> dataClass) {
      this.modelClass = modelClass;
      this.dataClass = dataClass;
    }

    public static synchronized <T> WaitModel<T> waitOn(T model) {
      @SuppressWarnings("unchecked") ModelLoaderFactory<WaitModel, InputStream> streamFactory =
          new Factory<>((Class<T>) model.getClass(), InputStream.class);
      Glide.get(InstrumentationRegistry.getTargetContext())
          .getRegistry()
          .replace(WaitModel.class, InputStream.class, streamFactory);

      return new WaitModel<>(model);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ModelLoader<WaitModel, Data> build(
        MultiModelLoaderFactory multiFactory) {
      WaitModelLoader<Model, Data> result =
          new WaitModelLoader<>(multiFactory.build(modelClass, dataClass));
      return (ModelLoader<WaitModel, Data>) (ModelLoader<?, ?>) result;
    }

    @Override
    public void teardown() {
      // Do nothing.
    }
  }

  private static final class WaitFetcher<Data> implements DataFetcher<Data> {

    private final DataFetcher<Data> wrapped;
    private CountDownLatch toWaitOn;

    WaitFetcher(DataFetcher<Data> wrapped, CountDownLatch toWaitOn) {
      this.wrapped = wrapped;
      this.toWaitOn = toWaitOn;
    }

    @Override
    public void loadData(Priority priority, DataCallback<? super Data> callback) {
      try {
        toWaitOn.await(ConcurrencyHelper.TIMEOUT_MS, ConcurrencyHelper.TIMEOUT_UNIT);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
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
