package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.util.ExceptionCatchingInputStream;
import com.bumptech.glide.util.MarkEnforcingInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Decodes {@link android.graphics.Bitmap Bitmaps} from {@link java.io.InputStream InputStreams}.
 */
public class StreamBitmapDecoder implements ResourceDecoder<InputStream, Bitmap> {

  private final Downsampler downsampler;
  private final ArrayPool byteArrayPool;

  public StreamBitmapDecoder(Downsampler downsampler, ArrayPool byteArrayPool) {
    this.downsampler = downsampler;
    this.byteArrayPool = byteArrayPool;
  }

  @Override
  public boolean handles(InputStream source, Options options) throws IOException {
    return downsampler.handles(source);
  }

  @Override
  public Resource<Bitmap> decode(InputStream source, int width, int height, Options options)
      throws IOException {

    // Use to fix the mark limit to avoid allocating buffers that fit entire images.
    final RecyclableBufferedInputStream bufferedStream;
    final boolean ownsBufferedStream;
    if (source instanceof RecyclableBufferedInputStream) {
      bufferedStream = (RecyclableBufferedInputStream) source;
      ownsBufferedStream = false;
    } else {
      bufferedStream = new RecyclableBufferedInputStream(source, byteArrayPool);
      ownsBufferedStream = true;
    }

    // Use to retrieve exceptions thrown while reading.
    // TODO(#126): when the framework no longer returns partially decoded Bitmaps or provides a
    // way to determine if a Bitmap is partially decoded, consider removing.
    ExceptionCatchingInputStream exceptionStream =
        ExceptionCatchingInputStream.obtain(bufferedStream);

    // Use to read data.
    // Ensures that we can always reset after reading an image header so that we can still
    // attempt to decode the full image even when the header decode fails and/or overflows our read
    // buffer. See #283.
    MarkEnforcingInputStream invalidatingStream = new MarkEnforcingInputStream(exceptionStream);
    UntrustedCallbacks callbacks = new UntrustedCallbacks(bufferedStream, exceptionStream);
    try {
      return downsampler.decode(invalidatingStream, width, height, options, callbacks);
    } finally {
      exceptionStream.release();
      if (ownsBufferedStream) {
        bufferedStream.release();
      }
    }
  }

  /**
   * Callbacks that provide reasonable handling for streams that may be unbuffered or insufficiently
   * buffered or that may throw exceptions during decoding.
   */
  static class UntrustedCallbacks implements Downsampler.DecodeCallbacks {
    private final RecyclableBufferedInputStream bufferedStream;
    private final ExceptionCatchingInputStream exceptionStream;

    public UntrustedCallbacks(RecyclableBufferedInputStream bufferedStream,
        ExceptionCatchingInputStream exceptionStream) {
      this.bufferedStream = bufferedStream;
      this.exceptionStream = exceptionStream;
    }

    @Override
    public void onObtainBounds() {
      // Once we've read the image header, we no longer need to allow the buffer to expand in
      // size. To avoid unnecessary allocations reading image data, we fix the mark limit so that it
      // is no larger than our current buffer size here. See issue #225.
      bufferedStream.fixMarkLimit();
    }

    @Override
    public void onDecodeComplete(BitmapPool bitmapPool, Bitmap downsampled) throws IOException {
      // BitmapFactory swallows exceptions during decodes and in some cases when inBitmap is non
      // null, may catch and log a stack trace but still return a non null bitmap. To avoid
      // displaying partially decoded bitmaps, we catch exceptions reading from the stream in our
      // ExceptionCatchingInputStream and throw them here.
      IOException streamException = exceptionStream.getException();
      if (streamException != null) {
        if (downsampled != null) {
          bitmapPool.put(downsampled);
        }
        throw streamException;
      }
    }
  }
}
