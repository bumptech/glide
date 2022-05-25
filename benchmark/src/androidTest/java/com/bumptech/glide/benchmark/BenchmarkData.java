package com.bumptech.glide.benchmark;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.benchmark.GlideBenchmarkRule.AfterStep;
import com.bumptech.glide.benchmark.GlideBenchmarkRule.BeforeStep;
import com.bumptech.glide.benchmark.GlideBenchmarkRule.LoadStep;
import com.bumptech.glide.benchmark.data.DataOpener;
import com.bumptech.glide.benchmark.data.DataOpener.ByteArrayBufferOpener;
import com.bumptech.glide.benchmark.data.DataOpener.ParcelFileDescriptorOpener;
import com.bumptech.glide.benchmark.data.DataOpener.StreamOpener;
import com.bumptech.glide.testutil.MockModelLoader;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BenchmarkData {
  private final int smallResourceId = R.raw.small;
  private final int hugeHeaderResourceId = R.raw.huge_header;
  @Rule public final GlideBenchmarkRule glideBenchmarkRule = new GlideBenchmarkRule();

  @Test
  public void smallAsStream() throws Exception {
    benchmarkData(new StreamOpener(), smallResourceId);
  }

  @Test
  public void hugeHeaderAsStream() throws Exception {
    benchmarkData(new StreamOpener(), hugeHeaderResourceId);
  }

  @Test
  public void smallAsByteArrayBuffer() throws Exception {
    benchmarkData(new ByteArrayBufferOpener(), smallResourceId);
  }

  @Test
  public void hugeHeaderAsByteArrayBuffer() throws Exception {
    benchmarkData(new ByteArrayBufferOpener(), hugeHeaderResourceId);
  }

  @Test
  public void smallAsFileDescriptor() throws Exception {
    benchmarkData(new ParcelFileDescriptorOpener(), smallResourceId);
  }

  @Test
  public void hugeHeaderAsFileDescriptor() throws Exception {
    benchmarkData(new ParcelFileDescriptorOpener(), hugeHeaderResourceId);
  }

  static final class ModelAndData<DataT> {
    private final Object model;
    private final DataT data;

    ModelAndData(Object model, DataT data) {
      this.model = model;
      this.data = data;
    }
  }

  private <T> void benchmarkData(final DataOpener<T> opener, final int resourceId)
      throws Exception {
    glideBenchmarkRule.runBenchmark(
        new BeforeStep<ModelAndData<T>>() {
          @Override
          public ModelAndData<T> act() throws IOException {
            FakeModel fakeModel = new FakeModel();
            T data = opener.acquire(resourceId);
            MockModelLoader.mock(fakeModel, data);
            return new ModelAndData<T>(fakeModel, data);
          }
        },
        new LoadStep<ModelAndData<T>>() {
          @Override
          public Object getModel(ModelAndData<T> beforeData) {
            return beforeData.model;
          }
        },
        new AfterStep<ModelAndData<T>>() {
          @Override
          public void act(ModelAndData<T> beforeData) throws IOException {
            opener.close(beforeData.data);
          }
        });
  }

  private static final class FakeModel {}
}
