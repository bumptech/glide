package com.bumptech.glide.gifdecoder;

import android.util.Log;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import static com.bumptech.glide.gifdecoder.GifDecoder.STATUS_FORMAT_ERROR;

public class GifHeaderParser {
    public static final String TAG = "GifHeaderParser";
    /**
     * Max decoder pixel stack size.
     */
    private static final int MAX_STACK_SIZE = 4096;

    private ByteBuffer rawData;
    private GifHeader header;

    // Raw data read working array.
    private byte[] block = new byte[256];
    // Block size last graphic control extension info.
    private int blockSize = 0;
    private short[] prefix;
    private byte[] suffix;
    private byte[] pixelStack;

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

    private void reset() {
        rawData = null;
        Arrays.fill(block, (byte) 0);
        header = new GifHeader();
        blockSize = 0;
        prefix = null;
        suffix = null;
        pixelStack = null;
    }

    public GifHeader parseHeader() {
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
    protected void readContents() {
        // Read GIF file content blocks.
        boolean done = false;
        while (!(done || err())) {
            int code = read();
            switch (code) {
                // Image separator.
                case 0x2C:
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
    protected void readGraphicControlExt() {
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
        header.isTransparent |= header.currentFrame.transparency;
        // Delay in milliseconds.
        header.currentFrame.delay = readShort() * 10;
        // Transparent color index
        header.currentFrame.transIndex = read();
        // Block terminator
        read();
    }

    /**
     * Reads next frame image.
     */
    protected void readBitmap() {
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
        skipBitmapData();

        skip();
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
    protected void readNetscapeExt() {
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
    protected void readLSD() {
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
    protected int[] readColorTable(int ncolors) {
        int nbytes = 3 * ncolors;
        int[] tab = null;
        byte[] c = new byte[nbytes];

        try {
            rawData.get(c);

            // Max size to avoid bounds checks.
            tab = new int[256];
            int i = 0;
            int j = 0;
            while (i < ncolors) {
                int r = ((int) c[j++]) & 0xff;
                int g = ((int) c[j++]) & 0xff;
                int b = ((int) c[j++]) & 0xff;
                tab[i++] = 0xff000000 | (r << 16) | (g << 8) | b;
            }
        } catch (BufferUnderflowException e) {
            Log.w(TAG, "Format Error Reading Color Table", e);
            header.status = STATUS_FORMAT_ERROR;
        }

        return tab;
    }

    /**
     * Decodes LZW image data into pixel array. Adapted from John Cristy's BitmapMagick.
     */
    protected void skipBitmapData() {
        int nullCode = -1;
        int npix = header.width * header.height;
        int available, clear, codeMask, codeSize, endOfInformation, inCode, oldCode, bits, code, count, i, datum,
                dataSize, first, top, bi;

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
        datum = bits = count = first = top = bi = 0;
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
            i++;
        }
    }

    /**
     * Skips variable length blocks up to and including next zero length block.
     */
    protected void skip() {
        do {
            readBlock();
        } while ((blockSize > 0) && !err());
    }

    /**
     * Reads next variable length block from input.
     *
     * @return number of bytes stored in "buffer"
     */
    protected int readBlock() {
        blockSize = read();
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
    protected int readShort() {
        // Read 16-bit value.
        return rawData.getShort();
    }

    private boolean err() {
        return header.status != GifDecoder.STATUS_OK;
    }
}
