package com.bumptech.glide.gifdecoder;

/**
 * Copyright (c) 2013 Xcellent Creations, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Reads frame data from a GIF image source and decodes it into individual frames for animation
 * purposes.  Image data can be read from either and InputStream source or a byte[].
 *
 * <p>This class is optimized for running animations with the frames, there are no methods to get
 * individual frame images, only to decode the next frame in the animation sequence.  Instead, it
 * lowers its memory footprint by only housing the minimum data necessary to decode the next frame
 * in the animation sequence.
 *
 * <p>The animation must be manually moved forward using {@link #advance()} before requesting the
 * next frame.  This method must also be called before you request the first frame or an error
 * will occur.
 *
 * <p>Implementation adapted from sample code published in Lyons. (2004). <em>Java for
 * Programmers</em>, republished under the MIT Open Source License
 */
public class GifDecoder {
  private static final String TAG = GifDecoder.class.getSimpleName();

  /**
   * File read status: No errors.
   */
  public static final int STATUS_OK = 0;
  /**
   * File read status: Error decoding file (may be partially decoded).
   */
  public static final int STATUS_FORMAT_ERROR = 1;
  /**
   * File read status: Unable to open source.
   */
  public static final int STATUS_OPEN_ERROR = 2;
  /**
   * Unable to fully decode the current frame.
   */
  public static final int STATUS_PARTIAL_DECODE = 3;
  /**
   * max decoder pixel stack size.
   */
  private static final int MAX_STACK_SIZE = 4096;

  /**
   * GIF Disposal Method meaning take no action.
   */
  private static final int DISPOSAL_UNSPECIFIED = 0;
  /**
   * GIF Disposal Method meaning leave canvas from previous frame.
   */
  private static final int DISPOSAL_NONE = 1;
  /**
   * GIF Disposal Method meaning clear canvas to background color.
   */
  private static final int DISPOSAL_BACKGROUND = 2;
  /**
   * GIF Disposal Method meaning clear canvas to frame before last.
   */
  private static final int DISPOSAL_PREVIOUS = 3;

  private static final int NULL_CODE = -1;

  private static final int INITIAL_FRAME_POINTER = -1;

  private static final int BYTES_PER_INTEGER = 4;

  // Global File Header values and parsing flags.
  // Active color table.
  private int[] act;

  // Raw GIF data from input source.
  private ByteBuffer rawData;

  // Raw data read working array.
  private byte[] block;

  // Temporary buffer for block reading. Reads 16k chunks from the native buffer for processing,
  // to greatly reduce JNI overhead.
  private static final int WORK_BUFFER_SIZE = 16384;
  @Nullable private byte[] workBuffer;
  private int workBufferSize = 0;
  private int workBufferPosition = 0;

  private GifHeaderParser parser;

  // LZW decoder working arrays.
  private short[] prefix;
  private byte[] suffix;
  private byte[] pixelStack;
  private byte[] mainPixels;
  private int[] mainScratch;

  private int framePointer;
  private GifHeader header;
  private BitmapProvider bitmapProvider;
  private Bitmap previousImage;
  private boolean savePrevious;
  private int status;
  private int sampleSize;
  private int downsampledHeight;
  private int downsampledWidth;
  private boolean isFirstFrameTransparent;

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
     * @param size
     */
    int[] obtainIntArray(int size);

    /**
     * Release the given array back to the pool.
     * @param array
     */
    void release(int[] array);
  }

  public GifDecoder(BitmapProvider provider, GifHeader gifHeader, ByteBuffer rawData) {
    this(provider, gifHeader, rawData, 1 /*sampleSize*/);
  }

  public GifDecoder(BitmapProvider provider, GifHeader gifHeader, ByteBuffer rawData,
      int sampleSize) {
    this(provider);
    setData(gifHeader, rawData, sampleSize);
  }

  public GifDecoder(BitmapProvider provider) {
    this.bitmapProvider = provider;
    header = new GifHeader();
  }

  public int getWidth() {
    return header.width;
  }

  public int getHeight() {
    return header.height;
  }

  public ByteBuffer getData() {
    return rawData;
  }

  /**
   * Returns the current status of the decoder.
   *
   * <p> Status will update per frame to allow the caller to tell whether or not the current frame
   * was decoded successfully and/or completely. Format and open failures persist across frames.
   * </p>
   */
  public int getStatus() {
    return status;
  }

  /**
   * Move the animation frame counter forward.
   */
  public void advance() {
    framePointer = (framePointer + 1) % header.frameCount;
  }

  /**
   * Gets display duration for specified frame.
   *
   * @param n int index of frame.
   * @return delay in milliseconds.
   */
  public int getDelay(int n) {
    int delay = -1;
    if ((n >= 0) && (n < header.frameCount)) {
      delay = header.frames.get(n).delay;
    }
    return delay;
  }

  /**
   * Gets display duration for the upcoming frame in ms.
   */
  public int getNextDelay() {
    if (header.frameCount <= 0 || framePointer < 0) {
      return 0;
    }

    return getDelay(framePointer);
  }

  /**
   * Gets the number of frames read from file.
   *
   * @return frame count.
   */
  public int getFrameCount() {
    return header.frameCount;
  }

  /**
   * Gets the current index of the animation frame, or -1 if animation hasn't not yet started.
   *
   * @return frame index.
   */
  public int getCurrentFrameIndex() {
    return framePointer;
  }

  /**
   * Resets the frame pointer to before the 0th frame, as if we'd never used this decoder to
   * decode any frames.
   */
  public void resetFrameIndex() {
    framePointer = INITIAL_FRAME_POINTER;
  }

  /**
   * Gets the "Netscape" iteration count, if any. A count of 0 means repeat indefinitely.
   *
   * @return iteration count if one was specified, else 1.
   */
  public int getLoopCount() {
    return header.loopCount;
  }

  /**
   * Returns an estimated byte size for this decoder based on the data provided to {@link
   * #setData(GifHeader, byte[])}, as well as internal buffers.
   */
  public int getByteSize() {
    return rawData.limit() + mainPixels.length + (mainScratch.length * BYTES_PER_INTEGER);
  }

  /**
   * Get the next frame in the animation sequence.
   *
   * @return Bitmap representation of frame.
   */
  public synchronized Bitmap getNextFrame() {
    if (header.frameCount <= 0 || framePointer < 0) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "unable to decode frame, frameCount=" + header.frameCount + " framePointer="
            + framePointer);
      }
      status = STATUS_FORMAT_ERROR;
    }
    if (status == STATUS_FORMAT_ERROR || status == STATUS_OPEN_ERROR) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Unable to decode frame, status=" + status);
      }
      return null;
    }
    status = STATUS_OK;

    GifFrame currentFrame = header.frames.get(framePointer);
    GifFrame previousFrame;
    int previousIndex = framePointer - 1;
    if (previousIndex >= 0) {
      previousFrame = header.frames.get(previousIndex);
    } else {
      previousFrame = header.frames.get(getFrameCount() - 1);
    }

    final int savedBgColor = header.bgColor;

    // Set the appropriate color table.
    if (currentFrame.lct == null) {
      act = header.gct;
    } else {
      act = currentFrame.lct;
      if (header.bgIndex == currentFrame.transIndex) {
        header.bgColor = 0;
      }
    }

    int save = 0;
    if (currentFrame.transparency) {
      save = act[currentFrame.transIndex];
      // Set transparent color if specified.
      act[currentFrame.transIndex] = 0;
    }
    if (act == null) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "No Valid Color Table");
      }
      // No color table defined.
      status = STATUS_FORMAT_ERROR;
      return null;
    }

    // Transfer pixel data to image.
    Bitmap result = setPixels(currentFrame, previousFrame);

    // Reset the transparent pixel in the color table
    if (currentFrame.transparency) {
      act[currentFrame.transIndex] = save;
    }
    header.bgColor = savedBgColor;

    return result;
  }

  /**
   * Reads GIF image from stream.
   *
   * @param is containing GIF file.
   * @return read status code (0 = no errors).
   */
  public int read(InputStream is, int contentLength) {
    if (is != null) {
      try {
        int capacity = (contentLength > 0) ? (contentLength + 4096) : 16384;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(capacity);
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
          buffer.write(data, 0, nRead);
        }
        buffer.flush();

        read(buffer.toByteArray());
      } catch (IOException e) {
        Log.w(TAG, "Error reading data from stream", e);
      }
    } else {
      status = STATUS_OPEN_ERROR;
    }

    try {
      if (is != null) {
        is.close();
      }
    } catch (IOException e) {
      Log.w(TAG, "Error closing stream", e);
    }

    return status;
  }

  public void clear() {
    header = null;
    if (mainPixels != null) {
      bitmapProvider.release(mainPixels);
    }
    if (mainScratch != null) {
      bitmapProvider.release(mainScratch);
    }
    if (previousImage != null) {
      bitmapProvider.release(previousImage);
    }
    previousImage = null;
    rawData = null;
    isFirstFrameTransparent = false;
    if (block != null) {
      bitmapProvider.release(block);
    }
    if (workBuffer != null) {
      bitmapProvider.release(workBuffer);
    }
  }

  public synchronized void setData(GifHeader header, byte[] data) {
    setData(header, ByteBuffer.wrap(data));
  }

  public synchronized void setData(GifHeader header, ByteBuffer buffer) {
    setData(header, buffer, 1);
  }

  public synchronized void setData(GifHeader header, ByteBuffer buffer, int sampleSize) {
    if (sampleSize <= 0) {
      throw new IllegalArgumentException("Sample size must be >=0, not: " + sampleSize);
    }
    // Make sure sample size is a power of 2.
    sampleSize = Integer.highestOneBit(sampleSize);
    this.status = STATUS_OK;
    this.header = header;
    isFirstFrameTransparent = false;
    framePointer = INITIAL_FRAME_POINTER;
    // Initialize the raw data buffer.
    rawData = buffer.asReadOnlyBuffer();
    rawData.position(0);
    rawData.order(ByteOrder.LITTLE_ENDIAN);

    // No point in specially saving an old frame if we're never going to use it.
    savePrevious = false;
    for (GifFrame frame : header.frames) {
      if (frame.dispose == DISPOSAL_PREVIOUS) {
        savePrevious = true;
        break;
      }
    }

    this.sampleSize = sampleSize;
    // Now that we know the size, init scratch arrays.
    // TODO: Find a way to avoid this entirely or at least downsample it
    // (either should be possible).
    mainPixels = bitmapProvider.obtainByteArray(header.width * header.height);
    mainScratch =
        bitmapProvider.obtainIntArray((header.width / sampleSize) * (header.height / sampleSize));
    downsampledWidth = header.width / sampleSize;
    downsampledHeight = header.height / sampleSize;
  }

  private GifHeaderParser getHeaderParser() {
    if (parser == null) {
      parser = new GifHeaderParser();
    }
    return parser;
  }

  /**
   * Reads GIF image from byte array.
   *
   * @param data containing GIF file.
   * @return read status code (0 = no errors).
   */
  public synchronized int read(byte[] data) {
    this.header = getHeaderParser().setData(data).parseHeader();
    if (data != null) {
      setData(header, data);
    }

    return status;
  }

  /**
   * Creates new frame image from current data (and previous frames as specified by their
   * disposition codes).
   */
  private Bitmap setPixels(GifFrame currentFrame, GifFrame previousFrame) {
    // Final location of blended pixels.
    final int[] dest = mainScratch;

    // fill in starting image contents based on last image's dispose code
    if (previousFrame != null && previousFrame.dispose > DISPOSAL_UNSPECIFIED) {
      // We don't need to do anything for DISPOSAL_NONE, if it has the correct pixels so will our
      // mainScratch and therefore so will our dest array.
      if (previousFrame.dispose == DISPOSAL_BACKGROUND) {
        // Start with a canvas filled with the background color
        int c = 0;
        if (!currentFrame.transparency) {
          c = header.bgColor;
        } else if (framePointer == 0) {
          // TODO: We should check and see if all individual pixels are replaced. If they are, the
          // first frame isn't actually transparent. For now, it's simpler and safer to assume
          // drawing a transparent background means the GIF contains transparency.
          isFirstFrameTransparent = true;
        }
        Arrays.fill(dest, c);
      } else if (previousFrame.dispose == DISPOSAL_PREVIOUS && previousImage != null) {
        // Start with the previous frame
        previousImage.getPixels(dest, 0, downsampledWidth, 0, 0, downsampledWidth,
            downsampledHeight);
      }
    }

    // Decode pixels for this frame  into the global pixels[] scratch.
    decodeBitmapData(currentFrame);

    int downsampledIH = currentFrame.ih / sampleSize;
    int downsampledIY = currentFrame.iy / sampleSize;
    int downsampledIW = currentFrame.iw / sampleSize;
    int downsampledIX = currentFrame.ix / sampleSize;
    // Copy each source line to the appropriate place in the destination.
    int pass = 1;
    int inc = 8;
    int iline = 0;
    boolean isFirstFrame = framePointer == 0;
    for (int i = 0; i < downsampledIH; i++) {
      int line = i;
      if (currentFrame.interlace) {
        if (iline >= downsampledIH) {
          pass++;
          switch (pass) {
            case 2:
              iline = 4;
              break;
            case 3:
              iline = 2;
              inc = 4;
              break;
            case 4:
              iline = 1;
              inc = 2;
              break;
            default:
              break;
          }
        }
        line = iline;
        iline += inc;
      }
      line += downsampledIY;
      if (line < downsampledHeight) {
        int k = line * downsampledWidth;
        // Start of line in dest.
        int dx = k + downsampledIX;
        // End of dest line.
        int dlim = dx + downsampledIW;
        if (k + downsampledWidth < dlim) {
          // Past dest edge.
          dlim = k + downsampledWidth;
        }
        // Start of line in source.
        int sx = i * sampleSize * currentFrame.iw;
        int maxPositionInSource = sx + ((dlim - dx) * sampleSize);
        while (dx < dlim) {
          // Map color and insert in destination.
          int averageColor = averageColorsNear(sx, maxPositionInSource, currentFrame.iw);
          if (averageColor != 0) {
            dest[dx] = averageColor;
          } else if (!isFirstFrameTransparent && isFirstFrame) {
            isFirstFrameTransparent = true;
          }
          sx += sampleSize;
          dx++;
        }
      }
    }

    // Copy pixels into previous image
    if (savePrevious && (currentFrame.dispose == DISPOSAL_UNSPECIFIED
        || currentFrame.dispose == DISPOSAL_NONE)) {
      if (previousImage == null) {
        previousImage = getNextBitmap();
      }
      previousImage.setPixels(dest, 0, downsampledWidth, 0, 0, downsampledWidth,
          downsampledHeight);
    }

    // Set pixels for current image.
    Bitmap result = getNextBitmap();
    result.setPixels(dest, 0, downsampledWidth, 0, 0, downsampledWidth, downsampledHeight);
    return result;
  }

  private int averageColorsNear(int positionInMainPixels, int maxPositionInMainPixels,
      int currentFrameIw) {
    int alphaSum = 0;
    int redSum = 0;
    int greenSum = 0;
    int blueSum = 0;

    int totalAdded = 0;
    // Find the pixels in the current row.
    for (int i = positionInMainPixels;
        i < positionInMainPixels + sampleSize && i < mainPixels.length
        && i < maxPositionInMainPixels; i++) {
      int currentColorIndex = ((int) mainPixels[i]) & 0xff;
      int currentColor = act[currentColorIndex];
      if (currentColor != 0) {
        alphaSum += currentColor >> 24 & 0x000000ff;
        redSum += currentColor >> 16 & 0x000000ff;
        greenSum += currentColor >> 8 & 0x000000ff;
        blueSum += currentColor & 0x000000ff;
        totalAdded++;
      }
    }
    // Find the pixels in the next row.
    for (int i = positionInMainPixels + currentFrameIw;
        i < positionInMainPixels + currentFrameIw + sampleSize && i < mainPixels.length
        && i < maxPositionInMainPixels; i++) {
      int currentColorIndex = ((int) mainPixels[i]) & 0xff;
      int currentColor = act[currentColorIndex];
      if (currentColor != 0) {
        alphaSum += currentColor >> 24 & 0x000000ff;
        redSum += currentColor >> 16 & 0x000000ff;
        greenSum += currentColor >> 8 & 0x000000ff;
        blueSum += currentColor & 0x000000ff;
        totalAdded++;
      }
    }
    if (totalAdded == 0) {
      return 0;
    } else {
      return ((alphaSum / totalAdded) << 24)
          | ((redSum / totalAdded) << 16)
          | ((greenSum / totalAdded) << 8)
          | (blueSum / totalAdded);
     }
  }

  /**
   * Decodes LZW image data into pixel array. Adapted from John Cristy's BitmapMagick.
   */
  private void decodeBitmapData(GifFrame frame) {
    workBufferSize = 0;
    workBufferPosition = 0;
    if (frame != null) {
      // Jump to the frame start position.
      rawData.position(frame.bufferFrameStart);
    }

    int npix = (frame == null) ? header.width * header.height : frame.iw * frame.ih;
    int available, clear, codeMask, codeSize, endOfInformation, inCode, oldCode, bits, code, count,
        i, datum,
        dataSize, first, top, bi, pi;

    if (mainPixels == null || mainPixels.length < npix) {
      // Allocate new pixel array.
      mainPixels = bitmapProvider.obtainByteArray(npix);
    }
    if (prefix == null) {
      prefix = new short[MAX_STACK_SIZE];
    }
    if (suffix == null) {
      suffix = new byte[MAX_STACK_SIZE];
    }
    if (pixelStack == null) {
      pixelStack = new byte[MAX_STACK_SIZE + 1];
    }

    // Initialize GIF data stream decoder.
    dataSize = readByte();
    clear = 1 << dataSize;
    endOfInformation = clear + 1;
    available = clear + 2;
    oldCode = NULL_CODE;
    codeSize = dataSize + 1;
    codeMask = (1 << codeSize) - 1;
    for (code = 0; code < clear; code++) {
      // XXX ArrayIndexOutOfBoundsException.
      prefix[code] = 0;
      suffix[code] = (byte) code;
    }

    // Decode GIF pixel stream.
    datum = bits = count = first = top = pi = bi = 0;
    for (i = 0; i < npix; ) {
      // Load bytes until there are enough bits for a code.
      if (count == 0) {
        // Read a new data block.
        count = readBlock();
        if (count <= 0) {
          status = STATUS_PARTIAL_DECODE;
          break;
        }
        bi = 0;
      }

      datum += (((int) block[bi]) & 0xff) << bits;
      bits += 8;
      bi++;
      count--;

      while (bits >= codeSize) {
        // Get the next code.
        code = datum & codeMask;
        datum >>= codeSize;
        bits -= codeSize;

        // Interpret the code.
        if (code == clear) {
          // Reset decoder.
          codeSize = dataSize + 1;
          codeMask = (1 << codeSize) - 1;
          available = clear + 2;
          oldCode = NULL_CODE;
          continue;
        }

        if (code > available) {
          status = STATUS_PARTIAL_DECODE;
          break;
        }

        if (code == endOfInformation) {
          break;
        }

        if (oldCode == NULL_CODE) {
          pixelStack[top++] = suffix[code];
          oldCode = code;
          first = code;
          continue;
        }
        inCode = code;
        if (code >= available) {
          pixelStack[top++] = (byte) first;
          code = oldCode;
        }
        while (code >= clear) {
          pixelStack[top++] = suffix[code];
          code = prefix[code];
        }
        first = ((int) suffix[code]) & 0xff;
        pixelStack[top++] = (byte) first;

        // Add a new string to the string table.
        if (available < MAX_STACK_SIZE) {
          prefix[available] = (short) oldCode;
          suffix[available] = (byte) first;
          available++;
          if (((available & codeMask) == 0) && (available < MAX_STACK_SIZE)) {
            codeSize++;
            codeMask += available;
          }
        }
        oldCode = inCode;

        while (top > 0) {
          // Pop a pixel off the pixel stack.
          mainPixels[pi++] = pixelStack[--top];
          i++;
        }
      }
    }

    // Clear missing pixels.
    for (i = pi; i < npix; i++) {
      mainPixels[i] = 0;
    }
  }

  /**
   * Reads the next chunk for the intermediate work buffer.
   */
  private void readChunkIfNeeded() {
    if (workBufferSize > workBufferPosition) {
      return;
    }
    if (workBuffer == null) {
      workBuffer = bitmapProvider.obtainByteArray(WORK_BUFFER_SIZE);
    }
    workBufferPosition = 0;
    workBufferSize = Math.min(rawData.remaining(), WORK_BUFFER_SIZE);
    rawData.get(workBuffer, 0, workBufferSize);
  }

  /**
   * Reads a single byte from the input stream.
   */
  private int readByte() {
    try {
      readChunkIfNeeded();
      return workBuffer[workBufferPosition++] & 0xFF;
    } catch (Exception e) {
      status = STATUS_FORMAT_ERROR;
      return 0;
    }
  }

  /**
   * Reads next variable length block from input.
   *
   * @return number of bytes stored in "buffer".
   */
  private int readBlock() {
    int blockSize = readByte();
    if (blockSize > 0) {
      try {
        if (block == null) {
          block = bitmapProvider.obtainByteArray(255);
        }
        final int remaining = workBufferSize - workBufferPosition;
        if (remaining >= blockSize) {
          // Block can be read from the current work buffer.
          System.arraycopy(workBuffer, workBufferPosition, block, 0, blockSize);
          workBufferPosition += blockSize;
        } else if (rawData.remaining() + remaining >= blockSize) {
          // Block can be read in two passes.
          System.arraycopy(workBuffer, workBufferPosition, block, 0, remaining);
          workBufferPosition = workBufferSize;
          readChunkIfNeeded();
          final int secondHalfRemaining = blockSize - remaining;
          System.arraycopy(workBuffer, 0, block, remaining, secondHalfRemaining);
          workBufferPosition += secondHalfRemaining;
        } else {
          status = STATUS_FORMAT_ERROR;
        }
      } catch (Exception e) {
        Log.w(TAG, "Error Reading Block", e);
        status = STATUS_FORMAT_ERROR;
      }
    }
    return blockSize;
  }

  private Bitmap getNextBitmap() {
    Bitmap.Config config = isFirstFrameTransparent
        ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
    Bitmap result = bitmapProvider.obtain(downsampledWidth, downsampledHeight, config);
    setAlpha(result);
    return result;
  }

  @TargetApi(12)
  private static void setAlpha(Bitmap bitmap) {
    if (Build.VERSION.SDK_INT >= 12) {
      bitmap.setHasAlpha(true);
    }
  }
}
