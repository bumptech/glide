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

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Reads frame data from a GIF image source and decodes it into individual frames
 * for animation purposes.  Image data can be read from either and InputStream source
 * or a byte[].
 *
 * This class is optimized for running animations with the frames, there
 * are no methods to get individual frame images, only to decode the next frame in the
 * animation sequence.  Instead, it lowers its memory footprint by only housing the minimum
 * data necessary to decode the next frame in the animation sequence.
 *
 * The animation must be manually moved forward using {@link #advance()} before requesting the next
 * frame.  This method must also be called before you request the first frame or an error will
 * occur.
 *
 * Implementation adapted from sample code published in Lyons. (2004). <em>Java for Programmers</em>,
 * republished under the MIT Open Source License
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

    // Global File Header values and parsing flags.
    // Active color table.
    private int[] act;

    // Raw GIF data from input source.
    private ByteBuffer rawData;

    // Raw data read working array.
    private byte[] block = new byte[256];
    // LZW decoder working arrays.
    private short[] prefix;
    private byte[] suffix;
    private byte[] pixelStack;
    private byte[] mainPixels;
    private int[] mainScratch;

    private int framePointer = -1;
    private byte[] data;
    private GifHeader header;
    private String id;
    private BitmapProvider bitmapProvider;
    private GifHeaderParser parser = new GifHeaderParser();
    private Bitmap previousImage;
    private boolean savePrevious;
    private Bitmap.Config config;

    /**
     * An interface that can be used to provide reused {@link android.graphics.Bitmap}s to avoid GCs from constantly
     * allocating {@link android.graphics.Bitmap}s for every frame.
     */
    public interface BitmapProvider {
        /**
         * Returns an {@link Bitmap} with exactly the given dimensions and config, or null if no such {@link Bitmap}
         * could be obtained.
         *
         * @param width The width of the desired {@link android.graphics.Bitmap}.
         * @param height The height of the desired {@link android.graphics.Bitmap}.
         * @param config The {@link android.graphics.Bitmap.Config} of the desired {@link android.graphics.Bitmap}.
         */
        public Bitmap obtain(int width, int height, Bitmap.Config config);
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

    public boolean isTransparent() {
        return header.isTransparent;
    }

    public byte[] getData() {
        return data;
    }

    public void setPreferredConfig(Bitmap.Config config) {
        this.config = config;
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
     * Gets display duration for the upcoming frame.
     */
    public int getNextDelay() {
        if (header.frameCount <= 0 || framePointer < 0) {
            return -1;
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
     * Gets the "Netscape" iteration count, if any. A count of 0 means repeat indefinitely.
     *
     * @return iteration count if one was specified, else 1.
     */
    public int getLoopCount() {
        return header.loopCount;
    }

    public String getId() {
        return id;
    }

    /**
     * Get the next frame in the animation sequence.
     *
     * @return Bitmap representation of frame.
     */
    public Bitmap getNextFrame() {
        if (header.frameCount <= 0 || framePointer < 0) {
            return null;
        }

        GifFrame frame = header.frames.get(framePointer);

        // Set the appropriate color table.
        if (frame.lct == null) {
            act = header.gct;
        } else {
            act = frame.lct;
            if (header.bgIndex == frame.transIndex) {
                header.bgColor = 0;
            }
        }

        int save = 0;
        if (frame.transparency) {
            save = act[frame.transIndex];
            // Set transparent color if specified.
            act[frame.transIndex] = 0;
        }
        if (act == null) {
            Log.w(TAG, "No Valid Color Table");
            // No color table defined.
            header.status = STATUS_FORMAT_ERROR;
            return null;
        }

        // Transfer pixel data to image.
        Bitmap result = setPixels(framePointer);

        // Reset the transparent pixel in the color table
        if (frame.transparency) {
            act[frame.transIndex] = save;
        }

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
            header.status = STATUS_OPEN_ERROR;
        }

        try {
            if (is != null) {
                is.close();
            }
        } catch (IOException e) {
            Log.w(TAG, "Error closing stream", e);
        }

        return header.status;
    }

    public void clear() {
        id = null;
        header = null;
        data = null;
        mainPixels = null;
        mainScratch = null;
    }

    public void setData(GifHeader header, byte[] data) {
        setData(null, header, data);
    }

    public void setData(String id, GifHeader header, byte[] data) {
        this.id = id;
        this.header = header;
        this.data = data;
        // Initialize the raw data buffer.
        rawData = ByteBuffer.wrap(data);
        rawData.rewind();
        rawData.order(ByteOrder.LITTLE_ENDIAN);

        // No point in specially saving an old frame if we're never going to use it.
        savePrevious = false;
        for (GifFrame frame : header.frames) {
            if (frame.dispose == DISPOSAL_PREVIOUS) {
                savePrevious = true;
                break;
            }
        }

        // Now that we know the size, init scratch arrays.
        mainPixels = new byte[header.width * header.height];
        mainScratch = new int[header.width * header.height];
    }

    /**
     * Reads GIF image from byte array.
     *
     * @param data containing GIF file.
     * @return read status code (0 = no errors).
     */
    public int read(byte[] data) {
        this.data = data;
        this.header = parser.setData(data).parseHeader();
        if (data != null) {
            // Initialize the raw data buffer.
            rawData = ByteBuffer.wrap(data);
            rawData.rewind();
            rawData.order(ByteOrder.LITTLE_ENDIAN);

            // Now that we know the size, init scratch arrays.
            mainPixels = new byte[header.width * header.height];
            mainScratch = new int[header.width * header.height];

            // No point in specially saving an old frame if we're never going to use it.
            savePrevious = false;
            for (GifFrame frame : header.frames) {
                if (frame.dispose == DISPOSAL_PREVIOUS) {
                    savePrevious = true;
                    break;
                }
            }
        }

        return header.status;
    }

    /**
     * Creates new frame image from current data (and previous frames as specified by their disposition codes).
     */
    private Bitmap setPixels(int frameIndex) {
        GifFrame currentFrame = header.frames.get(frameIndex);
        GifFrame previousFrame = null;
        int previousIndex = frameIndex - 1;
        if (previousIndex >= 0) {
            previousFrame = header.frames.get(previousIndex);
        }
        int width = header.width;
        int height = header.height;

        // Final location of blended pixels.
        final int[] dest = mainScratch;

        // fill in starting image contents based on last image's dispose code
        if (previousFrame != null && previousFrame.dispose > DISPOSAL_UNSPECIFIED) {
            // We don't need to do anything for DISPOSAL_NONE, if it has the correct pixels so will our mainScratch
            // and therefore so will our dest array.
            if (previousFrame.dispose == DISPOSAL_BACKGROUND) {
                // Start with a canvas filled with the background color
                int c = 0;
                if (!currentFrame.transparency) {
                    c = header.bgColor;
                }
                Arrays.fill(dest, c);
            } else if (previousFrame.dispose == DISPOSAL_PREVIOUS && previousImage != null) {
                // Start with the previous frame
                previousImage.getPixels(dest, 0, width, 0, 0, width, height);
            }
        }

        // Decode pixels for this frame  into the global pixels[] scratch.
        decodeBitmapData(currentFrame);

        // Copy each source line to the appropriate place in the destination.
        int pass = 1;
        int inc = 8;
        int iline = 0;
        for (int i = 0; i < currentFrame.ih; i++) {
            int line = i;
            if (currentFrame.interlace) {
                if (iline >= currentFrame.ih) {
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
            line += currentFrame.iy;
            if (line < header.height) {
                int k = line * header.width;
                // Start of line in dest.
                int dx = k + currentFrame.ix;
                // End of dest line.
                int dlim = dx + currentFrame.iw;
                if ((k + header.width) < dlim) {
                    // Past dest edge.
                    dlim = k + header.width;
                }
                // Start of line in source.
                int sx = i * currentFrame.iw;
                while (dx < dlim) {
                    // Map color and insert in destination.
                    int index = ((int) mainPixels[sx++]) & 0xff;
                    int c = act[index];
                    if (c != 0) {
                        dest[dx] = c;
                    }
                    dx++;
                }
            }
        }

        //Copy pixels into previous image
        if (savePrevious && currentFrame.dispose == DISPOSAL_UNSPECIFIED || currentFrame.dispose == DISPOSAL_NONE) {
            if (previousImage == null) {
                previousImage = getNextBitmap();
                previousImage.setPixels(dest, 0, width, 0, 0, width, height);
            }
        }

        // Set pixels for current image.
        Bitmap result = getNextBitmap();
        result.setPixels(dest, 0, width, 0, 0, width, height);
        return result;
    }

    /**
     * Decodes LZW image data into pixel array. Adapted from John Cristy's BitmapMagick.
     */
    private void decodeBitmapData(GifFrame frame) {
        if (frame != null) {
            // Jump to the frame start position.
            rawData.position(frame.bufferFrameStart);
        }

        int nullCode = -1;
        int npix = (frame == null) ? header.width * header.height : frame.iw * frame.ih;
        int available, clear, codeMask, codeSize, endOfInformation, inCode, oldCode, bits, code, count, i, datum,
                dataSize, first, top, bi, pi;

        if (mainPixels == null || mainPixels.length < npix) {
            // Allocate new pixel array.
            mainPixels = new byte[npix];
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
        dataSize = read();
        clear = 1 << dataSize;
        endOfInformation = clear + 1;
        available = clear + 2;
        oldCode = nullCode;
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
            if (top == 0) {
                if (bits < codeSize) {
                    // Load bytes until there are enough bits for a code.
                    if (count == 0) {
                        // Read a new data block.
                        count = readBlock();
                        if (count <= 0) {
                            break;
                        }
                        bi = 0;
                    }
                    datum += (((int) block[bi]) & 0xff) << bits;
                    bits += 8;
                    bi++;
                    count--;
                    continue;
                }
                // Get the next code.
                code = datum & codeMask;
                datum >>= codeSize;
                bits -= codeSize;
                // Interpret the code.
                if ((code > available) || (code == endOfInformation)) {
                    break;
                }
                if (code == clear) {
                    // Reset decoder.
                    codeSize = dataSize + 1;
                    codeMask = (1 << codeSize) - 1;
                    available = clear + 2;
                    oldCode = nullCode;
                    continue;
                }
                if (oldCode == nullCode) {
                    pixelStack[top++] = suffix[code];
                    oldCode = code;
                    first = code;
                    continue;
                }
                inCode = code;
                if (code == available) {
                    pixelStack[top++] = (byte) first;
                    code = oldCode;
                }
                while (code > clear) {
                    pixelStack[top++] = suffix[code];
                    code = prefix[code];
                }
                first = ((int) suffix[code]) & 0xff;
                // Add a new string to the string table.
                if (available >= MAX_STACK_SIZE) {
                    break;
                }
                pixelStack[top++] = (byte) first;
                prefix[available] = (short) oldCode;
                suffix[available] = (byte) first;
                available++;
                if (((available & codeMask) == 0) && (available < MAX_STACK_SIZE)) {
                    codeSize++;
                    codeMask += available;
                }
                oldCode = inCode;
            }
            // Pop a pixel off the pixel stack.
            top--;
            mainPixels[pi++] = pixelStack[top];
            i++;
        }

        // Clear missing pixels.
        for (i = pi; i < npix; i++) {
            mainPixels[i] = 0;
        }
    }

    /**
     * Reads a single byte from the input stream.
     */
    private int read() {
        int curByte = 0;
        try {
            curByte = rawData.get() & 0xFF;
        } catch (Exception e) {
            header.status = STATUS_FORMAT_ERROR;
        }
        return curByte;
    }

    /**
     * Reads next variable length block from input.
     *
     * @return number of bytes stored in "buffer".
     */
    private int readBlock() {
        int blockSize = read();
        int n = 0;
        if (blockSize > 0) {
            try {
                int count;
                while (n < blockSize) {
                    count = blockSize - n;
                    rawData.get(block, n, count);

                    n += count;
                }
            } catch (Exception e) {
                Log.w(TAG, "Error Reading Block", e);
                header.status = STATUS_FORMAT_ERROR;
            }
        }
        return n;
    }

    private Bitmap.Config getPreferredConfig() {
        if (config == Bitmap.Config.RGB_565 && !header.isTransparent) {
            return Bitmap.Config.RGB_565;
        } else {
            return Bitmap.Config.ARGB_8888;
        }
    }

    private Bitmap getNextBitmap() {
        Bitmap.Config targetConfig = getPreferredConfig();
        Bitmap result = bitmapProvider.obtain(header.width, header.height, targetConfig);
        if (result == null) {
            result = Bitmap.createBitmap(header.width, header.height, targetConfig);
        } else {
            // If we're reusing a bitmap it may have other things drawn in it which we need to remove.
            result.eraseColor(Color.TRANSPARENT);
        }
        return result;
    }
}
