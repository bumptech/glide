package com.bumptech.glide.gifdecoder;

import static com.bumptech.glide.gifdecoder.GifDecoder.STATUS_FORMAT_ERROR;

import android.util.Log;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * A class responsible for creating {@link com.bumptech.glide.gifdecoder.GifHeader}s from data representing animated
 * gifs.
 */
public class GifHeaderParser {
    public static final String TAG = "GifHeaderParser";

    // The minimum frame delay in hundredths of a second.
    static final int MIN_FRAME_DELAY = 3;
    // The default frame delay in hundredths of a second for GIFs with frame delays less than the minimum.
    static final int DEFAULT_FRAME_DELAY = 10;

    private static final int MAX_BLOCK_SIZE = 256;
    // Raw data read working array.
    private final byte[] block = new byte[MAX_BLOCK_SIZE];

    private ByteBuffer rawData;
    private GifHeader header;
    private int blockSize = 0;

    public GifHeaderParser setData(byte[] data) {
        reset();
        if (data != null) {
            rawData = ByteBuffer.wrap(data);
            rawData.rewind();
            rawData.order(ByteOrder.LITTLE_ENDIAN);
        } else {
            rawData = null;
            header.status = GifDecoder.STATUS_OPEN_ERROR;
        }
        return this;
    }

    public void clear() {
        rawData = null;
        header = null;
    }

    private void reset() {
        rawData = null;
        Arrays.fill(block, (byte) 0);
        header = new GifHeader();
        blockSize = 0;
    }

    public GifHeader parseHeader() {
        if (rawData == null) {
            throw new IllegalStateException("You must call setData() before parseHeader()");
        }
        if (err()) {
            return header;
        }

        readHeader();
        if (!err()) {
            readContents();
            if (header.frameCount < 0) {
                header.status = STATUS_FORMAT_ERROR;
            }
        }

        return header;
    }

    /**
     * Main file parser. Reads GIF content blocks.
     */
    private void readContents() {
        // Read GIF file content blocks.
        boolean done = false;
        while (!(done || err())) {
            int code = read();
            switch (code) {
                // Image separator.
                case 0x2C:
                    // The graphics control extension is optional, but will always come first if it exists. If one did
                    // exist, there will be a non-null current frame which we should use. However if one did not exist,
                    // the current frame will be null and we must create it here. See issue #134.
                    if (header.currentFrame == null) {
                        header.currentFrame = new GifFrame();
                    }
                    readBitmap();
                    break;
                // Extension.
                case 0x21:
                    code = read();
                    switch (code) {
                        // Graphics control extension.
                        case 0xf9:
                            // Start a new frame.
                            header.currentFrame = new GifFrame();
                            readGraphicControlExt();
                            break;
                        // Application extension.
                        case 0xff:
                            readBlock();
                            String app = "";
                            for (int i = 0; i < 11; i++) {
                                app += (char) block[i];
                            }
                            if (app.equals("NETSCAPE2.0")) {
                                readNetscapeExt();
                            } else {
                                // Don't care.
                                skip();
                            }
                            break;
                        // Comment extension.
                        case 0xfe:
                            skip();
                            break;
                        // Plain text extension.
                        case 0x01:
                            skip();
                            break;
                        // Uninteresting extension.
                        default:
                            skip();
                    }
                    break;
                // Terminator.
                case 0x3b:
                    done = true;
                    break;
                // Bad byte, but keep going and see what happens break;
                case 0x00:
                default:
                    header.status = STATUS_FORMAT_ERROR;
            }
        }
    }

    /**
     * Reads Graphics Control Extension values.
     */
    private void readGraphicControlExt() {
        // Block size.
        read();
        // Packed fields.
        int packed = read();
        // Disposal method.
        header.currentFrame.dispose = (packed & 0x1c) >> 2;
        if (header.currentFrame.dispose == 0) {
            // Elect to keep old image if discretionary.
            header.currentFrame.dispose = 1;
        }
        header.currentFrame.transparency = (packed & 1) != 0;
        // Delay in milliseconds.
        int delayInHundredthsOfASecond = readShort();
        // TODO: consider allowing -1 to indicate show forever.
        if (delayInHundredthsOfASecond < MIN_FRAME_DELAY) {
            delayInHundredthsOfASecond = DEFAULT_FRAME_DELAY;
        }
        header.currentFrame.delay = delayInHundredthsOfASecond * 10;
        // Transparent color index
        header.currentFrame.transIndex = read();
        // Block terminator
        read();
    }

    /**
     * Reads next frame image.
     */
    private void readBitmap() {
        // (sub)image position & size.
        header.currentFrame.ix = readShort();
        header.currentFrame.iy = readShort();
        header.currentFrame.iw = readShort();
        header.currentFrame.ih = readShort();

        int packed = read();
        // 1 - local color table flag interlace
        boolean lctFlag = (packed & 0x80) != 0;
        int lctSize = (int) Math.pow(2, (packed & 0x07) + 1);
        // 3 - sort flag
        // 4-5 - reserved lctSize = 2 << (packed & 7); // 6-8 - local color
        // table size
        header.currentFrame.interlace = (packed & 0x40) != 0;
        if (lctFlag) {
            // Read table.
            header.currentFrame.lct = readColorTable(lctSize);
        } else {
            // No local color table.
            header.currentFrame.lct = null;
        }

        // Save this as the decoding position pointer.
        header.currentFrame.bufferFrameStart = rawData.position();

        // False decode pixel data to advance buffer.
        skipImageData();

        if (err()) {
            return;
        }

        header.frameCount++;
        // Add image to frame.
        header.frames.add(header.currentFrame);
    }
    /**
     * Reads Netscape extension to obtain iteration count.
     */
    private void readNetscapeExt() {
        do {
            readBlock();
            if (block[0] == 1) {
                // Loop count sub-block.
                int b1 = ((int) block[1]) & 0xff;
                int b2 = ((int) block[2]) & 0xff;
                header.loopCount = (b2 << 8) | b1;
            }
        } while ((blockSize > 0) && !err());
    }


    /**
     * Reads GIF file header information.
     */
    private void readHeader() {
        String id = "";
        for (int i = 0; i < 6; i++) {
            id += (char) read();
        }
        if (!id.startsWith("GIF")) {
            header.status = STATUS_FORMAT_ERROR;
            return;
        }
        readLSD();
        if (header.gctFlag && !err()) {
            header.gct = readColorTable(header.gctSize);
            header.bgColor = header.gct[header.bgIndex];
        }
    }
    /**
     * Reads Logical Screen Descriptor.
     */
    private void readLSD() {
        // Logical screen size.
        header.width = readShort();
        header.height = readShort();
        // Packed fields
        int packed = read();
        // 1 : global color table flag.
        header.gctFlag = (packed & 0x80) != 0;
        // 2-4 : color resolution.
        // 5 : gct sort flag.
        // 6-8 : gct size.
        header.gctSize = 2 << (packed & 7);
        // Background color index.
        header.bgIndex = read();
        // Pixel aspect ratio
        header.pixelAspect = read();
    }

    /**
     * Reads color table as 256 RGB integer values.
     *
     * @param ncolors int number of colors to read.
     * @return int array containing 256 colors (packed ARGB with full alpha).
     */
    private int[] readColorTable(int ncolors) {
        int nbytes = 3 * ncolors;
        int[] tab = null;
        byte[] c = new byte[nbytes];

        try {
            rawData.get(c);

            // TODO: what bounds checks are we avoiding if we know the number of colors?
            // Max size to avoid bounds checks.
            tab = new int[MAX_BLOCK_SIZE];
            int i = 0;
            int j = 0;
            while (i < ncolors) {
                int r = ((int) c[j++]) & 0xff;
                int g = ((int) c[j++]) & 0xff;
                int b = ((int) c[j++]) & 0xff;
                tab[i++] = 0xff000000 | (r << 16) | (g << 8) | b;
            }
        } catch (BufferUnderflowException e) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Format Error Reading Color Table", e);
            }
            header.status = STATUS_FORMAT_ERROR;
        }

        return tab;
    }

    /**
     * Skips LZW image data for a single frame to advance buffer.
     */
    private void skipImageData() {
        // lzwMinCodeSize
        read();
        // data sub-blocks
        skip();
    }

    /**
     * Skips variable length blocks up to and including next zero length block.
     */
    private void skip() {
        int blockSize;
        do {
            blockSize = read();
            rawData.position(rawData.position() + blockSize);
        } while (blockSize > 0);
    }

    /**
     * Reads next variable length block from input.
     *
     * @return number of bytes stored in "buffer"
     */
    private int readBlock() {
        blockSize = read();
        int n = 0;
        if (blockSize > 0) {
            int count = 0;
            try {
                while (n < blockSize) {
                    count = blockSize - n;
                    rawData.get(block, n, count);

                    n += count;
                }
            } catch (Exception e) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Error Reading Block n: " + n + " count: " + count + " blockSize: " + blockSize, e);
                }
                header.status = STATUS_FORMAT_ERROR;
            }
        }
        return n;
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
     * Reads next 16-bit value, LSB first.
     */
    private int readShort() {
        // Read 16-bit value.
        return rawData.getShort();
    }

    private boolean err() {
        return header.status != GifDecoder.STATUS_OK;
    }
}
