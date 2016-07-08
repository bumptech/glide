package com.bumptech.glide.gifdecoder;

import android.graphics.Bitmap;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;

/**
 * Shared interface for GIF decoders.
 */
public interface GifDecoder {
  /** File read status: No errors. */
  int STATUS_OK = 0;
  /** File read status: Error decoding file (may be partially decoded). */
  int STATUS_FORMAT_ERROR = 1;
  /** File read status: Unable to open source. */
  int STATUS_OPEN_ERROR = 2;
  /** Unable to fully decode the current frame. */
  int STATUS_PARTIAL_DECODE = 3;

  /** Android Lint annotation for status codes that can be used with a GIF decoder. */
  @Retention(RetentionPolicy.SOURCE)
  @IntDef(value = {STATUS_OK, STATUS_FORMAT_ERROR, STATUS_OPEN_ERROR, STATUS_PARTIAL_DECODE})
  @interface GifDecodeStatus {
  }

  /**
   * An interface that can be used to provide reused {@link android.graphics.Bitmap}s to avoid GCs
   * from constantly allocating {@link android.graphics.Bitmap}s for every frame.
   */
  public interface BitmapProvider {
    /**
     * Returns an {@link Bitmap} with exactly the given dimensions and config.
     *
     * @param width  The width in pixels of the desired {@link android.graphics.Bitmap}.
     * @param height The height in pixels of the desired {@link android.graphics.Bitmap}.
     * @param config The {@link android.graphics.Bitmap.Config} of the desired {@link
     *               android.graphics.Bitmap}.
     */
    @NonNull
    Bitmap obtain(int width, int height, Bitmap.Config config);

    /**
     * Releases the given Bitmap back to the pool.
     */
    void release(Bitmap bitmap);

    /**
     * Returns a byte array used for decoding and generating the frame bitmap.
     *
     * @param size the size of the byte array to obtain
     */
    byte[] obtainByteArray(int size);

    /**
     * Releases the given byte array back to the pool.
     */
    void release(byte[] bytes);

    /**
     * Returns an int array used for decoding/generating the frame bitmaps.
     */
    int[] obtainIntArray(int size);

    /**
     * Release the given array back to the pool.
     */
    void release(int[] array);
  }

  int getWidth();

  int getHeight();

  ByteBuffer getData();

  /**
   * Returns the current status of the decoder.
   *
   * <p> Status will update per frame to allow the caller to tell whether or not the current frame
   * was decoded successfully and/or completely. Format and open failures persist across frames.
   * </p>
   */
  @GifDecodeStatus
  int getStatus();

  /**
   * Move the animation frame counter forward.
   */
  void advance();

  /**
   * Gets display duration for specified frame.
   *
   * @param n int index of frame.
   * @return delay in milliseconds.
   */
  int getDelay(int n);

  /**
   * Gets display duration for the upcoming frame in ms.
   */
  int getNextDelay();

  /**
   * Gets the number of frames read from file.
   *
   * @return frame count.
   */
  int getFrameCount();

  /**
   * Gets the current index of the animation frame, or -1 if animation hasn't not yet started.
   *
   * @return frame index.
   */
  int getCurrentFrameIndex();

  /**
   * Resets the frame pointer to before the 0th frame, as if we'd never used this decoder to
   * decode any frames.
   */
  void resetFrameIndex();

  /**
   * Gets the "Netscape" iteration count, if any. A count of 0 means repeat indefinitely.
   *
   * @return iteration count if one was specified, else 1.
   */
  int getLoopCount();

  /**
   * Returns an estimated byte size for this decoder based on the data provided to {@link
   * #setData(GifHeader, byte[])}, as well as internal buffers.
   */
  int getByteSize();

  /**
   * Get the next frame in the animation sequence.
   *
   * @return Bitmap representation of frame.
   */
  Bitmap getNextFrame();

  /**
   * Reads GIF image from stream.
   *
   * @param is containing GIF file.
   * @return read status code (0 = no errors).
   */
  @GifDecodeStatus
  int read(InputStream is, int contentLength);

  void clear();

  void setData(GifHeader header, byte[] data);

  void setData(GifHeader header, ByteBuffer buffer);

  void setData(GifHeader header, ByteBuffer buffer, int sampleSize);

  /**
   * Reads GIF image from byte array.
   *
   * @param data containing GIF file.
   * @return read status code (0 = no errors).
   */
  @GifDecodeStatus
  int read(byte[] data);

}
