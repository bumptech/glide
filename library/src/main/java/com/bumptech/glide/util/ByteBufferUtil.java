package com.bumptech.glide.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
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

  public static void encode(ByteBuffer byteBuffer, OutputStream os) throws IOException {
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
      toCopy.rewind();
      toCopy.get(result);
    }
    return result;
  }

  private static SafeArray getSafeArray(ByteBuffer byteBuffer) {
    if (!byteBuffer.isReadOnly() && byteBuffer.hasArray()) {
      return new SafeArray(byteBuffer.array(), byteBuffer.arrayOffset(), byteBuffer.limit());
    }
    return null;
  }

  static final class SafeArray {
    private final int offset;
    private final int limit;
    private final byte[] data;

    public SafeArray(byte[] data, int offset, int limit) {
      this.data = data;
      this.offset = offset;
      this.limit = limit;
    }
  }
}
