package com.bumptech.glide.integration.cronet;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.chromium.net.UrlResponseInfo;

/**
 * A utility for processing response bodies, as one contiguous buffer rather than an asynchronous
 * stream.
 */
final class BufferQueue {
  public static final String CONTENT_LENGTH = "content-length";
  public static final String CONTENT_ENCODING = "content-encoding";
  private final Queue<ByteBuffer> mBuffers;
  private final AtomicBoolean mIsCoalesced = new AtomicBoolean(false);

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Use this class during a request, to combine streamed buffers of a response into a single final
   * buffer.
   *
   * <p>For example: {@code @Override public void onResponseStarted(UrlRequest request,
   * UrlResponseInfo info) { request.read(builder.getFirstBuffer(info)); } @Override public void
   * onReadCompleted(UrlRequest request, UrlResponseInfo info, ByteBuffer buffer) {
   * request.read(builder.getNextBuffer(buffer)); } }
   */
  public static final class Builder {
    private ArrayDeque<ByteBuffer> mBuffers = new ArrayDeque<>();
    private RuntimeException whenClosed;

    private Builder() {}

    /** Returns the next buffer to write data into. */
    public ByteBuffer getNextBuffer(ByteBuffer lastBuffer) {
      if (mBuffers == null) {
        throw new RuntimeException(whenClosed);
      }
      if (lastBuffer != mBuffers.peekLast()) {
        mBuffers.addLast(lastBuffer);
      }
      if (lastBuffer.hasRemaining()) {
        return lastBuffer;
      } else {
        return ByteBuffer.allocateDirect(8096);
      }
    }

    /** Returns a ByteBuffer heuristically sized to hold the whole response body. */
    public ByteBuffer getFirstBuffer(UrlResponseInfo info) {
      // Security note - a malicious server could attempt to exhaust client memory by sending
      // down a Content-Length of a very large size, which we would eagerly allocate without
      // the server having to actually send those bytes. This isn't considered to be an
      // issue, because that same malicious server could use our transparent gzip to force us
      // to allocate 1032 bytes per byte sent by the server.
      return ByteBuffer.allocateDirect((int) Math.min(bufferSizeHeuristic(info), 524288));
    }

    private static long bufferSizeHeuristic(UrlResponseInfo info) {
      final Map<String, List<String>> headers = info.getAllHeaders();
      if (headers.containsKey(CONTENT_LENGTH)) {
        long contentLength = Long.parseLong(headers.get(CONTENT_LENGTH).get(0));
        boolean isCompressed =
            !headers.containsKey(CONTENT_ENCODING)
                || (headers.get(CONTENT_ENCODING).size() == 1
                    && "identity".equals(headers.get(CONTENT_ENCODING).get(0)));
        if (isCompressed) {
          // We have to guess at the uncompressed size. In the future, consider guessing a
          // compression ratio based on the content-type and content-encoding. For now,
          // assume 2.
          return 2 * contentLength;
        } else {
          // In this case, we know exactly how many bytes we're going to get, so we can
          // size our buffer perfectly. However, we still have to call read() for the last time,
          // even when we know there shouldn't be any more bytes coming. To avoid allocating another
          // buffer for that case, add one more byte than we really need.
          return contentLength + 1;
        }
      } else {
        // No content-length. This means we're either being sent a chunked response, or the
        // java stack stripped content length because of transparent gzip. In either case we really
        // have no idea, and so we fall back to a reasonable guess.
        return 8192;
      }
    }

    public BufferQueue build() {
      whenClosed = new RuntimeException();
      final ArrayDeque<ByteBuffer> buffers = mBuffers;
      mBuffers = null;
      return new BufferQueue(buffers);
    }
  }

  private BufferQueue(Queue<ByteBuffer> buffers) {
    mBuffers = buffers;
    for (ByteBuffer buffer : mBuffers) {
      buffer.flip();
    }
  }

  /** Returns the response body as a single contiguous buffer. */
  public ByteBuffer coalesceToBuffer() {
    markCoalesced();
    if (mBuffers.size() == 0) {
      return ByteBuffer.allocateDirect(0);
    } else if (mBuffers.size() == 1) {
      return mBuffers.remove();
    } else {
      int size = 0;
      for (ByteBuffer buffer : mBuffers) {
        size += buffer.remaining();
      }
      ByteBuffer result = ByteBuffer.allocateDirect(size);
      while (!mBuffers.isEmpty()) {
        result.put(mBuffers.remove());
      }
      result.flip();
      return result;
    }
  }

  private void markCoalesced() {
    if (!mIsCoalesced.compareAndSet(false, true)) {
      throw new IllegalStateException("This BufferQueue has already been consumed");
    }
  }
}
