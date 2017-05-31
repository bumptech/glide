package com.bumptech.glide.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utilities for interacting with {@link java.nio.ByteBuffer}s.
 */
public final class ByteBufferUtil {
  // 16 Kb
  private static final int BUFFER_SIZE = 16384;
  private static final AtomicReference<byte[]> BUFFER_REF = new AtomicReference<>();

  private ByteBufferUtil() {
    // Utility class.
  }

  public static ByteBuffer fromFile(File file) throws IOException {
    RandomAccessFile raf = null;
    FileChannel channel = null;
    try {
      raf = new RandomAccessFile(file, "r");
      channel = raf.getChannel();
      return channel.map(FileChannel.MapMode.READ_ONLY, 0, file.length()).load();
    } finally {
      if (channel != null) {
        try {
          channel.close();
        } catch (IOException e) {
          // Ignored.
        }
      }
      if (raf != null) {
        try {
          raf.close();
        } catch (IOException e) {
          // Ignored.
        }
      }
    }
  }

  public static void toFile(ByteBuffer buffer, File file) throws IOException {
    RandomAccessFile raf = null;
    FileChannel channel = null;
    try {
      raf = new RandomAccessFile(file, "rw");
      channel = raf.getChannel();
      channel.write(buffer);
      channel.force(false /*metadata*/);
      channel.close();
      raf.close();
    } finally {
      if (channel != null) {
        try {
          channel.close();
        } catch (IOException e) {
          // Ignored.
        }
      }
      if (raf != null) {
        try {
          raf.close();
        } catch (IOException e) {
          // Ignored.
        }
      }
    }
  }

  public static void toStream(ByteBuffer byteBuffer, OutputStream os) throws IOException {
    SafeArray safeArray = getSafeArray(byteBuffer);
    if (safeArray != null) {
      os.write(safeArray.data, safeArray.offset, safeArray.offset + safeArray.limit);
    } else {
      byte[] buffer = BUFFER_REF.getAndSet(null);
      if (buffer == null) {
        buffer = new byte[BUFFER_SIZE];
      }

      while (byteBuffer.remaining() > 0) {
        int toRead = Math.min(byteBuffer.remaining(), buffer.length);
        byteBuffer.get(buffer, 0 /*dstOffset*/, toRead /*byteCount*/);
        os.write(buffer, 0, toRead);
      }

      BUFFER_REF.set(buffer);
    }
  }

  public static byte[] toBytes(ByteBuffer byteBuffer) {
    final byte[] result;
    SafeArray safeArray = getSafeArray(byteBuffer);
    if (safeArray != null && safeArray.offset == 0 && safeArray.limit == safeArray.data.length) {
      result = byteBuffer.array();
    } else {
      ByteBuffer toCopy = byteBuffer.asReadOnlyBuffer();
      result = new byte[toCopy.limit()];
      toCopy.position(0);
      toCopy.get(result);
    }
    return result;
  }

  public static InputStream toStream(ByteBuffer buffer) {
    return new ByteBufferStream(buffer);
  }

  public static ByteBuffer fromStream(InputStream stream) throws IOException {
    ByteArrayOutputStream outStream = new ByteArrayOutputStream(BUFFER_SIZE);

    byte[] buffer = BUFFER_REF.getAndSet(null);
    if (buffer == null) {
      buffer = new byte[BUFFER_SIZE];
    }

    int n = -1;
    while ((n = stream.read(buffer)) >= 0) {
      outStream.write(buffer, 0, n);
    }

    BUFFER_REF.set(buffer);

    byte[] bytes = outStream.toByteArray();

    // Some resource decoders require a direct byte buffer. Prefer allocateDirect() over wrap()
    return (ByteBuffer) ByteBuffer.allocateDirect(bytes.length).put(bytes).position(0);
  }

  private static SafeArray getSafeArray(ByteBuffer byteBuffer) {
    if (!byteBuffer.isReadOnly() && byteBuffer.hasArray()) {
      return new SafeArray(byteBuffer.array(), byteBuffer.arrayOffset(), byteBuffer.limit());
    }
    return null;
  }

  static final class SafeArray {
    @Synthetic final int offset;
    @Synthetic final int limit;
    @Synthetic final byte[] data;

    public SafeArray(byte[] data, int offset, int limit) {
      this.data = data;
      this.offset = offset;
      this.limit = limit;
    }
  }

  private static class ByteBufferStream extends InputStream {
    private static final int UNSET = -1;
    private final ByteBuffer byteBuffer;
    private int markPos = UNSET;

    public ByteBufferStream(ByteBuffer byteBuffer) {
      this.byteBuffer = byteBuffer;
    }

    @Override
    public int available() throws IOException {
      return byteBuffer.remaining();
    }

    @Override
    public int read() throws IOException {
      if (!byteBuffer.hasRemaining()) {
        return -1;
      }
      return byteBuffer.get();
    }

    @Override
    public synchronized void mark(int readlimit) {
      markPos = byteBuffer.position();
    }

    @Override
    public boolean markSupported() {
      return true;
    }

    @Override
    public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
      if (!byteBuffer.hasRemaining()) {
        return -1;
      }
      int toRead = Math.min(byteCount, available());
      byteBuffer.get(buffer, byteOffset, toRead);
      return toRead;
    }

    @Override
    public synchronized void reset() throws IOException {
      if (markPos == UNSET) {
        throw new IOException("Cannot reset to unset mark position");
      }
      // reset() was not implemented correctly in 4.0.4, so we track the mark position ourselves.
      byteBuffer.position(markPos);
    }

    @Override
    public long skip(long byteCount) throws IOException {
      if (!byteBuffer.hasRemaining()) {
        return -1;
      }

      long toSkip = Math.min(byteCount, available());
      byteBuffer.position((int) (byteBuffer.position() + toSkip));
      return toSkip;
    }
  }
}
