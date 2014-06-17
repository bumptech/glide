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
     * File read status: Error decoding file (may be partially decoded)
     */
    public static final int STATUS_FORMAT_ERROR = 1;
    /**
     * File read status: Unable to open source.
     */
    public static final int STATUS_OPEN_ERROR = 2;
    /**
     * max decoder pixel stack size
     */
    private static final int MAX_STACK_SIZE = 4096;

    /**
     * GIF Disposal Method meaning take no action
     */
    private static final int DISPOSAL_UNSPECIFIED = 0;
    /**
     * GIF Disposal Method meaning leave canvas from previous frame
     */
    private static final int DISPOSAL_NONE = 1;
    /**
     * GIF Disposal Method meaning clear canvas to background color
     */
    private static final int DISPOSAL_BACKGROUND = 2;
    /**
     * GIF Disposal Method meaning clear canvas to frame before last
     */
    private static final int DISPOSAL_PREVIOUS = 3;

    //Global File Header values and parsing flags
    private int[] act; // active color table

    // Raw GIF data from input source
    private ByteBuffer rawData;

    // Raw data read working array
    private byte[] block = new byte[256]; // current data block
    // LZW decoder working arrays
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

    public interface BitmapProvider {
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

    public int getGifByteSize() {
        return data.length;
    }

    public byte[] getData() {
        return data;
    }

    public int getDecodedFramesByteSizeSum() {
        // 4 == ARGB_8888, 2 == RGB_565
        return header.frameCount * header.width * header.height * (header.isTransparent ? 4 : 2);
    }

    /**
     * Move the animation frame counter forward
     */
    public void advance() {
        framePointer = (framePointer + 1) % header.frameCount;
    }

    /**
     * Gets display duration for specified frame.
     *
     * @param n int index of frame
     * @return delay in milliseconds
     */
    public int getDelay(int n) {
        int delay = -1;
        if ((n >= 0) && (n < header.frameCount)) {
            delay = header.frames.get(n).delay;
        }
        return delay;
    }

    /**
     * Gets display duration for the upcoming frame
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
     * @return frame count
     */
    public int getFrameCount() {
        return header.frameCount;
    }

    /**
     * Gets the current index of the animation frame, or -1 if animation hasn't not yet started
     *
     * @return frame index
     */
    public int getCurrentFrameIndex() {
        return framePointer;
    }

    /**
     * Gets the "Netscape" iteration count, if any. A count of 0 means repeat indefinitiely.
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
     * @return Bitmap representation of frame
     */
    public Bitmap getNextFrame() {
        if (header.frameCount <= 0 || framePointer < 0 ) {
            return null;
        }

        GifFrame frame = header.frames.get(framePointer);

        //Set the appropriate color table
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
            act[frame.transIndex] = 0; // set transparent color if specified
        }
        if (act == null) {
            Log.w(TAG, "No Valid Color Table");
            header.status = STATUS_FORMAT_ERROR; // no color table defined
            return null;
        }

        Bitmap result = setPixels(framePointer); // transfer pixel data to image

        // Reset the transparent pixel in the color table
        if (frame.transparency) {
            act[frame.transIndex] = save;
        }

        return result;
    }

    /**
     * Reads GIF image from stream
     *
     * @param is containing GIF file.
     * @return read status code (0 = no errors)
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

    public void setData(String id, GifHeader header, byte[] data) {
        this.id = id;
        this.header = header;
        this.data = data;
        //Initialize the raw data buffer
        rawData = ByteBuffer.wrap(data);
        rawData.rewind();
        rawData.order(ByteOrder.LITTLE_ENDIAN);

        //Now that we know the size, init scratch arrays
        mainPixels = new byte[header.width * header.height];
        mainScratch = new int[header.width * header.height];
    }

    /**
     * Reads GIF image from byte array
     *
     * @param data containing GIF file.
     * @return read status code (0 = no errors)
     */
    public int read(byte[] data) {
        this.data = data;
        this.header = new GifHeaderParser(data).parseHeader();
        if (data != null) {
            //Initialize the raw data buffer
            rawData = ByteBuffer.wrap(data);
            rawData.rewind();
            rawData.order(ByteOrder.LITTLE_ENDIAN);

            //Now that we know the size, init scratch arrays
            mainPixels = new byte[header.width * header.height];
            mainScratch = new int[header.width * header.height];
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

        // final location of blended pixels
        final int[] dest = mainScratch;

        // fill in starting image contents based on last image's dispose code
        if (previousFrame != null && previousFrame.dispose > DISPOSAL_UNSPECIFIED) {
//            if (previousFrame.dispose == DISPOSAL_NONE) {
//                We don't need to do anything for this case, mainScratch should already have the pixels of the
//                previous image.
//                currentImage.getPixels(dest, 0, header.width, 0, 0, header.width, header.height);
//            }
            if (previousFrame.dispose == DISPOSAL_BACKGROUND) {
                // Start with a canvas filled with the background color
                int c = 0;
                if (!currentFrame.transparency) {
                    c = header.bgColor;
                }
                for (int i = 0; i < previousFrame.ih; i++) {
                    int n1 = (previousFrame.iy + i) * header.width + previousFrame.ix;
                    int n2 = n1 + previousFrame.iw;
                    for (int k = n1; k < n2; k++) {
                        dest[k] = c;
                    }
                }
            }
        } else {
            int c = 0;
            if (!currentFrame.transparency) {
                c = header.bgColor;
            }
            for (int i = 0; i < dest.length; i++) {
                dest[i] = c;
            }
        }

        // Decode pixels for this frame  into the global pixels[] scratch
        decodeBitmapData(currentFrame, mainPixels); // decode pixel data

        // copy each source line to the appropriate place in the destination
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
                int dx = k + currentFrame.ix; // start of line in dest
                int dlim = dx + currentFrame.iw; // end of dest line
                if ((k + header.width) < dlim) {
                    dlim = k + header.width; // past dest edge
                }
                int sx = i * currentFrame.iw; // start of line in source
                while (dx < dlim) {
                    // map color and insert in destination
                    int index = ((int) mainPixels[sx++]) & 0xff;
                    int c = act[index];
                    if (c != 0) {
                        dest[dx] = c;
                    }
                    dx++;
                }
            }
        }

        //Set pixels for current image
        Bitmap result = getNextBitmap();
        result.setPixels(dest, 0, header.width, 0, 0, header.width, header.height);
        return result;
    }

    /**
     * Decodes LZW image data into pixel array. Adapted from John Cristy's BitmapMagick.
     */
    private void decodeBitmapData(GifFrame frame, byte[] dstPixels) {
        if (frame != null) {
            //Jump to the frame start position
            rawData.position(frame.bufferFrameStart);
        }

        int nullCode = -1;
        int npix = (frame == null) ? header.width * header.height : frame.iw * frame.ih;
        int available, clear, code_mask, code_size, end_of_information, in_code, old_code, bits, code, count, i, datum, data_size, first, top, bi, pi;

        if (dstPixels == null || dstPixels.length < npix) {
            dstPixels = new byte[npix]; // allocate new pixel array
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
        data_size = read();
        clear = 1 << data_size;
        end_of_information = clear + 1;
        available = clear + 2;
        old_code = nullCode;
        code_size = data_size + 1;
        code_mask = (1 << code_size) - 1;
        for (code = 0; code < clear; code++) {
            prefix[code] = 0; // XXX ArrayIndexOutOfBoundsException
            suffix[code] = (byte) code;
        }

        // Decode GIF pixel stream.
        datum = bits = count = first = top = pi = bi = 0;
        for (i = 0; i < npix; ) {
            if (top == 0) {
                if (bits < code_size) {
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
                code = datum & code_mask;
                datum >>= code_size;
                bits -= code_size;
                // Interpret the code
                if ((code > available) || (code == end_of_information)) {
                    break;
                }
                if (code == clear) {
                    // Reset decoder.
                    code_size = data_size + 1;
                    code_mask = (1 << code_size) - 1;
                    available = clear + 2;
                    old_code = nullCode;
                    continue;
                }
                if (old_code == nullCode) {
                    pixelStack[top++] = suffix[code];
                    old_code = code;
                    first = code;
                    continue;
                }
                in_code = code;
                if (code == available) {
                    pixelStack[top++] = (byte) first;
                    code = old_code;
                }
                while (code > clear) {
                    pixelStack[top++] = suffix[code];
                    code = prefix[code];
                }
                first = ((int) suffix[code]) & 0xff;
                // Add a new string to the string table,
                if (available >= MAX_STACK_SIZE) {
                    break;
                }
                pixelStack[top++] = (byte) first;
                prefix[available] = (short) old_code;
                suffix[available] = (byte) first;
                available++;
                if (((available & code_mask) == 0) && (available < MAX_STACK_SIZE)) {
                    code_size++;
                    code_mask += available;
                }
                old_code = in_code;
            }
            // Pop a pixel off the pixel stack.
            top--;
            dstPixels[pi++] = pixelStack[top];
            i++;
        }

        for (i = pi; i < npix; i++) {
            dstPixels[i] = 0; // clear missing pixels
        }
    }

    /**
     * Reads a single byte from the input stream.
     */
    private int read() {
        int curByte = 0;
        try {
            curByte = (rawData.get() & 0xFF);
        } catch (Exception e) {
            header.status = STATUS_FORMAT_ERROR;
        }
        return curByte;
    }

    /**
     * Reads next variable length block from input.
     *
     * @return number of bytes stored in "buffer"
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

    private Bitmap getNextBitmap() {
        Bitmap.Config targetConfig = header.isTransparent ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
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
