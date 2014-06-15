package com.bumptech.glide.gifdecoder;

import android.util.Log;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.bumptech.glide.gifdecoder.GifDecoder.STATUS_FORMAT_ERROR;

public class GifHeaderParser {
    public static final String TAG = "GifHeaderParser";
    /**
     * max decoder pixel stack size
     */
    private static final int MAX_STACK_SIZE = 4096;

    private final ByteBuffer rawData;
    private GifHeader header = new GifHeader();

    // Raw data read working array
    protected byte[] block = new byte[256]; // current data block
    protected int blockSize = 0; // block size last graphic control extension info
    protected boolean lctFlag; // local color table flag
    protected int lctSize; // local color table size
    private short[] prefix;
    private byte[] suffix;
    private byte[] pixelStack;

    public GifHeaderParser(byte[] data) {
        if (data != null) {
            rawData = ByteBuffer.wrap(data);
            rawData.rewind();
            rawData.order(ByteOrder.LITTLE_ENDIAN);
        } else {
            rawData = null;
            header.status = GifDecoder.STATUS_OPEN_ERROR;
        }
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
        // read GIF file content blocks
        boolean done = false;
        while (!(done || err())) {
            int code = read();
            switch (code) {
                case 0x2C: // image separator
                    readBitmap();
                    break;
                case 0x21: // extension
                    code = read();
                    switch (code) {
                        case 0xf9: // graphics control extension
                            //Start a new frame
                            header.currentFrame = new GifFrame();
                            readGraphicControlExt();
                            break;
                        case 0xff: // application extension
                            readBlock();
                            String app = "";
                            for (int i = 0; i < 11; i++) {
                                app += (char) block[i];
                            }
                            if (app.equals("NETSCAPE2.0")) {
                                readNetscapeExt();
                            } else {
                                skip(); // don't care
                            }
                            break;
                        case 0xfe:// comment extension
                            skip();
                            break;
                        case 0x01:// plain text extension
                            skip();
                            break;
                        default: // uninteresting extension
                            skip();
                    }
                    break;
                case 0x3b: // terminator
                    done = true;
                    break;
                case 0x00: // bad byte, but keep going and see what happens break;
                default:
                    header.status = STATUS_FORMAT_ERROR;
            }
        }
    }

    /**
     * Reads Graphics Control Extension values
     */
    protected void readGraphicControlExt() {
        read(); // block size
        int packed = read(); // packed fields
        header.currentFrame.dispose = (packed & 0x1c) >> 2; // disposal method
        if (header.currentFrame.dispose == 0) {
            header.currentFrame.dispose = 1; // elect to keep old image if discretionary
        }
        header.currentFrame.transparency = (packed & 1) != 0;
        header.isTransparent |= header.currentFrame.transparency;
        header.currentFrame.delay = readShort() * 10; // delay in milliseconds
        header.currentFrame.transIndex = read(); // transparent color index
        read(); // block terminator
    }

     /**
     * Reads next frame image
     */
    protected void readBitmap() {
        header.currentFrame.ix = readShort(); // (sub)image position & size
        header.currentFrame.iy = readShort();
        header.currentFrame.iw = readShort();
        header.currentFrame.ih = readShort();

        int packed = read();
        lctFlag = (packed & 0x80) != 0; // 1 - local color table flag interlace
        lctSize = (int) Math.pow(2, (packed & 0x07) + 1);
        // 3 - sort flag
        // 4-5 - reserved lctSize = 2 << (packed & 7); // 6-8 - local color
        // table size
        header.currentFrame.interlace = (packed & 0x40) != 0;
        if (lctFlag) {
            header.currentFrame.lct = readColorTable(lctSize); // read table
        } else {
            header.currentFrame.lct = null; //No local color table
        }

        header.currentFrame.bufferFrameStart = rawData.position(); //Save this as the decoding position pointer

        skipBitmapData();  // false decode pixel data to advance buffer.

        skip();
        if (err()) {
            return;
        }

        header.frameCount++;
        header.frames.add(header.currentFrame); // add image to frame
    }
       /**
     * Reads Netscape extenstion to obtain iteration count
     */
    protected void readNetscapeExt() {
        do {
            readBlock();
            if (block[0] == 1) {
                // loop count sub-block
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
     * Reads Logical Screen Descriptor
     */
    protected void readLSD() {
        // logical screen size
        header.width = readShort();
        header.height = readShort();
        // packed fields
        int packed = read();
        header.gctFlag = (packed & 0x80) != 0; // 1 : global color table flag
        // 2-4 : color resolution
        // 5 : gct sort flag
        header.gctSize = 2 << (packed & 7); // 6-8 : gct size
        header.bgIndex = read(); // background color index
        header.pixelAspect = read(); // pixel aspect ratio

        //Now that we know the size, init scratch arrays
        //TODO: these shouldn't go here.
//        mainPixels = new byte[header.width * header.height];
//        mainScratch = new int[header.width * header.height];
    }
     /**
     * Reads color table as 256 RGB integer values
     *
     * @param ncolors int number of colors to read
     * @return int array containing 256 colors (packed ARGB with full alpha)
     */
    protected int[] readColorTable(int ncolors) {
        int nbytes = 3 * ncolors;
        int[] tab = null;
        byte[] c = new byte[nbytes];

        try {
            rawData.get(c);

            tab = new int[256]; // max size to avoid bounds checks
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
        int available, clear, code_mask, code_size, end_of_information, in_code, old_code, bits, code, count, i, datum, data_size, first, top, bi, pi;

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
        long start = System.currentTimeMillis();
        for (code = 0; code < clear; code++) {
            prefix[code] = 0; // XXX ArrayIndexOutOfBoundsException
            suffix[code] = (byte) code;
        }

        start = System.currentTimeMillis();
        // Decode GIF pixel stream.
        datum = bits = count = first = top = pi = bi = 0;
        int iterations = 0;
        for (i = 0; i < npix; ) {
            iterations++;
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
            curByte = (rawData.get() & 0xFF);
        } catch (Exception e) {
            header.status = STATUS_FORMAT_ERROR;
        }
        return curByte;
    }

    /**
     * Reads next 16-bit value, LSB first
     */
    protected int readShort() {
        // read 16-bit value
        return rawData.getShort();
    }

    private boolean err() {
        return header.status != GifDecoder.STATUS_OK;
    }
}
