package com.bumptech.glide.benchmark.data;

import android.os.ParcelFileDescriptor;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.test.core.app.ApplicationProvider;
import com.bumptech.glide.util.ByteBufferUtil;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/** Converts test resources into various useful data types for benchmarking. */
public interface DataOpener<T> {
  T acquire(@RawRes int resourceId) throws IOException;

  void close(T data) throws IOException;

  final class StreamOpener implements DataOpener<InputStream> {

    @Override
    public InputStream acquire(@RawRes int resourceId) {
      return ApplicationProvider.getApplicationContext().getResources().openRawResource(resourceId);
    }

    @Override
    public void close(InputStream data) throws IOException {
      data.close();
    }
  }

  final class ByteArrayBufferOpener implements DataOpener<ByteBuffer> {

    @Override
    public ByteBuffer acquire(@RawRes int resourceId) throws IOException {
      InputStream is = null;
      try {
        is = new StreamOpener().acquire(resourceId);
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

  final class InputStreamOverByteArrayBufferOpener implements DataOpener<InputStream> {

    private final ByteArrayBufferOpener byteArrayBufferOpener = new ByteArrayBufferOpener();
    @Nullable private ByteBuffer buffer;

    @Override
    public InputStream acquire(@RawRes int resourceId) throws IOException {
      buffer = byteArrayBufferOpener.acquire(resourceId);
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

  final class FileOpener implements DataOpener<File> {

    @Override
    public File acquire(@RawRes int resourceId) throws IOException {
      ByteBuffer byteBuffer = new ByteArrayBufferOpener().acquire(resourceId);
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

  final class ByteArrayOpener implements DataOpener<byte[]> {

    @Override
    public byte[] acquire(@RawRes int resourceId) throws IOException {
      InputStream is = null;
      try {
        is = new StreamOpener().acquire(resourceId);
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

  final class ParcelFileDescriptorOpener implements DataOpener<ParcelFileDescriptor> {

    private final FileOpener fileOpener = new FileOpener();
    @Nullable private File file;

    @Override
    public ParcelFileDescriptor acquire(@RawRes int resourceId) throws IOException {
      file = fileOpener.acquire(resourceId);
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

  final class MemoryMappedByteBufferOpener implements DataOpener<ByteBuffer> {

    private final FileOpener fileOpener = new FileOpener();
    @Nullable private File file;

    @Override
    public ByteBuffer acquire(@RawRes int resourceId) throws IOException {
      file = fileOpener.acquire(resourceId);
      return ByteBufferUtil.fromFile(file);
    }

    @Override
    public void close(ByteBuffer data) {
      if (file != null) {
        fileOpener.close(file);
      }
    }
  }
}
