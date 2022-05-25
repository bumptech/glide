package com.bumptech.glide.load.resource.bitmap;

import android.app.Application;
import android.os.ParcelFileDescriptor;
import androidx.benchmark.BenchmarkState;
import androidx.benchmark.junit4.BenchmarkRule;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.benchmark.R;
import com.bumptech.glide.benchmark.data.DataOpener;
import com.bumptech.glide.benchmark.data.DataOpener.ByteArrayBufferOpener;
import com.bumptech.glide.benchmark.data.DataOpener.ByteArrayOpener;
import com.bumptech.glide.benchmark.data.DataOpener.FileOpener;
import com.bumptech.glide.benchmark.data.DataOpener.InputStreamOverByteArrayBufferOpener;
import com.bumptech.glide.benchmark.data.DataOpener.MemoryMappedByteBufferOpener;
import com.bumptech.glide.benchmark.data.DataOpener.ParcelFileDescriptorOpener;
import com.bumptech.glide.benchmark.data.DataOpener.StreamOpener;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.bitmap_recycle.LruArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.request.target.Target;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Benchmarks to compare the performance of the various data types supported by Downsampler. */
@RunWith(AndroidJUnit4.class)
public class BenchmarkDownsampler {
  private static final int SIZE = Target.SIZE_ORIGINAL;
  private static final int RESOURCE_ID = R.raw.pixel3a_portrait;
  private final Application app = ApplicationProvider.getApplicationContext();

  @Rule public BenchmarkRule mBenchmarkRule = new BenchmarkRule();

  @Test
  public void testInputStream() throws IOException {
    runBenchmark(new StreamOpener(), new InputStreamDecoder());
  }

  @Test
  public void testByteBufferOverByteArray() throws IOException {
    runBenchmark(new ByteArrayBufferOpener(), new ByteBufferDecoder());
  }

  @Test
  public void testByteBufferOverFile() throws IOException {
    runBenchmark(new MemoryMappedByteBufferOpener(), new ByteBufferDecoder());
  }

  @Test
  public void testParcelFileDescriptorOverFile() throws IOException {
    runBenchmark(new ParcelFileDescriptorOpener(), new ParcelFileDescriptorDecoder());
  }

  @Test
  public void testFile() throws IOException {
    runBenchmark(new FileOpener(), new FileDecoder());
  }

  @Test
  public void testByteArray() throws IOException {
    runBenchmark(new ByteArrayOpener(), new ByteArrayDecoder());
  }

  // A legacy case that's since been removed.
  @Test
  public void testInputStreamOverByteBufferOverByteArray() throws IOException {
    runBenchmark(new InputStreamOverByteArrayBufferOpener(), new InputStreamDecoder());
  }

  private <T> void runBenchmark(DataOpener<T> opener, Decoder<T> decoder) throws IOException {
    final BenchmarkState state = mBenchmarkRule.getState();
    while (state.keepRunning()) {
      state.pauseTiming();
      T data = null;
      try {
        data = opener.acquire(RESOURCE_ID);
        Downsampler downsampler = newDownsampler();
        state.resumeTiming();

        decoder.decode(downsampler, data, SIZE, SIZE);
      } finally {
        state.pauseTiming();
        opener.close(data);
        state.resumeTiming();
      }
    }
  }

  private interface Decoder<T> {
    void decode(Downsampler downsampler, T data, int width, int height) throws IOException;
  }

  private static final class ByteBufferDecoder implements Decoder<ByteBuffer> {
    @Override
    public void decode(Downsampler downsampler, ByteBuffer data, int width, int height)
        throws IOException {
      downsampler.decode(data, width, height, new Options());
    }
  }

  private static final class InputStreamDecoder implements Decoder<InputStream> {
    @Override
    public void decode(Downsampler downsampler, InputStream data, int width, int height)
        throws IOException {
      downsampler.decode(data, width, height, new Options());
    }
  }

  private static final class ParcelFileDescriptorDecoder implements Decoder<ParcelFileDescriptor> {
    @Override
    public void decode(Downsampler downsampler, ParcelFileDescriptor data, int width, int height)
        throws IOException {
      downsampler.decode(data, width, height, new Options());
    }
  }

  private static final class FileDecoder implements Decoder<File> {
    @Override
    public void decode(Downsampler downsampler, File data, int width, int height)
        throws IOException {
      downsampler.decode(data, width, height, new Options());
    }
  }

  private static final class ByteArrayDecoder implements Decoder<byte[]> {
    @Override
    public void decode(Downsampler downsampler, byte[] data, int width, int height)
        throws IOException {
      downsampler.decode(data, width, height, new Options());
    }
  }

  private Downsampler newDownsampler() {
    ImmutableList<ImageHeaderParser> imageHeaderParsers =
        ImmutableList.of(new DefaultImageHeaderParser(), new ExifInterfaceImageHeaderParser());
    return new Downsampler(
        imageHeaderParsers,
        app.getResources().getDisplayMetrics(),
        new LruBitmapPool(20 * 1024 * 1024),
        new LruArrayPool(5 * 1024 * 1024));
  }
}
