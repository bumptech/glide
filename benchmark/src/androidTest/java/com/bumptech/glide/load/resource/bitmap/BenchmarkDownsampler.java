package com.bumptech.glide.load.resource.bitmap;

import android.app.Application;
import android.os.ParcelFileDescriptor;
import androidx.annotation.Nullable;
import androidx.benchmark.BenchmarkState;
import androidx.benchmark.junit4.BenchmarkRule;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.bumptech.glide.benchmark.R;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.bitmap_recycle.LruArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.util.ByteBufferUtil;
import com.google.common.collect.ImmutableList;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Benchmarks to compare the performance of the various data types supported byDownsampler. */
@RunWith(AndroidJUnit4.class)
public class BenchmarkDownsampler {
  private static final int SIZE = Target.SIZE_ORIGINAL;
  private static final int RESOURCE_ID = R.raw.canonical;
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
        data = opener.acquire();
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

  private interface DataOpener<T> {
    T acquire() throws IOException;

    void close(T data) throws IOException;
  }

  private static final class StreamOpener implements DataOpener<InputStream> {

    @Override
    public InputStream acquire() {
      return ApplicationProvider.getApplicationContext()
          .getResources()
          .openRawResource(RESOURCE_ID);
    }

    @Override
    public void close(InputStream data) throws IOException {
      data.close();
    }
  }

  private static final class ByteArrayBufferOpener implements DataOpener<ByteBuffer> {

    @Override
    public ByteBuffer acquire() throws IOException {
      InputStream is = null;
      try {
        is = new StreamOpener().acquire();
        return ByteBufferUtil.fromStream(is);
      } finally {
        if (is != null) {
          try {
            is.close();
          } catch (IOException e) {
            // Ignored.
          }
        }
      }
    }

    @Override
    public void close(ByteBuffer data) {}
  }

  private static final class InputStreamOverByteArrayBufferOpener
      implements DataOpener<InputStream> {

    private final ByteArrayBufferOpener byteArrayBufferOpener = new ByteArrayBufferOpener();
    @Nullable private ByteBuffer buffer;

    @Override
    public InputStream acquire() throws IOException {
      buffer = byteArrayBufferOpener.acquire();
      return ByteBufferUtil.toStream(buffer);
    }

    @Override
    public void close(InputStream data) throws IOException {
      data.close();
      if (buffer != null) {
        byteArrayBufferOpener.close(buffer);
      }
    }
  }

  private static final class FileOpener implements DataOpener<File> {

    @Override
    public File acquire() throws IOException {
      ByteBuffer byteBuffer = new ByteArrayBufferOpener().acquire();
      File tempFile =
          File.createTempFile(
              "memory_mapped", "jpg", ApplicationProvider.getApplicationContext().getCacheDir());
      ByteBufferUtil.toFile(byteBuffer, tempFile);
      return tempFile;
    }

    @Override
    public void close(File data) {
      if (!data.delete()) {
        throw new IllegalStateException("Failed to delete: " + data);
      }
    }
  }

  private static final class ByteArrayOpener implements DataOpener<byte[]> {

    @Override
    public byte[] acquire() throws IOException {
      InputStream is = null;
      try {
        is = new StreamOpener().acquire();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024 * 1024];
        int read;
        while ((read = is.read(buffer, /* off= */ 0, buffer.length)) != -1) {
          outputStream.write(buffer, /* off= */ 0, read);
        }
        return outputStream.toByteArray();
      } finally {
        if (is != null) {
          try {
            is.close();
          } catch (IOException e) {
            // Ignored.
          }
        }
      }
    }

    @Override
    public void close(byte[] data) {}
  }

  private static final class ParcelFileDescriptorOpener
      implements DataOpener<ParcelFileDescriptor> {

    private final FileOpener fileOpener = new FileOpener();
    @Nullable private File file;

    @Override
    public ParcelFileDescriptor acquire() throws IOException {
      file = fileOpener.acquire();
      return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public void close(ParcelFileDescriptor data) throws IOException {
      data.close();
      if (file != null) {
        fileOpener.close(file);
      }
    }
  }

  private static final class MemoryMappedByteBufferOpener implements DataOpener<ByteBuffer> {

    private final FileOpener fileOpener = new FileOpener();
    @Nullable private File file;

    @Override
    public ByteBuffer acquire() throws IOException {
      file = fileOpener.acquire();
      return ByteBufferUtil.fromFile(file);
    }

    @Override
    public void close(ByteBuffer data) {
      if (file != null) {
        fileOpener.close(file);
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
