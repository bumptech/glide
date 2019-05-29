package com.bumptech.glide.gifencoder;


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Class AnimatedGifEncoder - Encodes a GIF file consisting of one or more
 * frames.
 *
 * <pre>
 *  Example:
 *     AnimatedGifEncoder e = new AnimatedGifEncoder();
 *     e.start(outputFileName);
 *     e.setDelay(1000);   // 1 frame per sec
 *     e.addFrame(image1);
 *     e.addFrame(image2);
 *     e.addFrame(image3, 100, 100);    // set position of the frame
 *     e.finish();
 * </pre>
 *
 * No copyright asserted on the source code of this class. May be used for any
 * purpose, however, refer to the Unisys LZW patent for restrictions on use of
 * the associated LZWEncoder class. Please forward any corrections to
 * kweiner@fmsware.com.
 *
 * @author Kevin Weiner, FM Software
 * @version 1.03 November 2003
 *
 */

public class AnimatedGifEncoder {
    private static final String TAG = "AnimatedGifEncoder";

    // The minimum % of an images pixels that must be transparent for us to set a transparent index
    // automatically.
    private static final double MIN_TRANSPARENT_PERCENTAGE = 4d;

    private int width; // image size

    private int height;

    private int fixedWidth;   // set by setSize()

    private int fixedHeight;

    private Integer transparent = null; // transparent color if given

    private int transIndex; // transparent index in color table

    private int repeat = -1; // no repeat

    private int delay = 0; // frame delay (hundredths)

    private boolean started = false; // ready to output frames

    private OutputStream out;

    private Bitmap image; // current frame

    private byte[] pixels; // BGR byte array from frame

    private byte[] indexedPixels; // converted frame indexed to palette

    private int colorDepth; // number of bit planes

    private byte[] colorTab; // RGB palette

    private boolean[] usedEntry = new boolean[256]; // active palette entries

    private int palSize = 7; // color table size (bits-1)

    private int dispose = -1; // disposal code (-1 = use default)

    private boolean closeStream = false; // close stream when finished

    private boolean firstFrame = true;

    private boolean sizeSet = false; // if false, get size from first frame

    private int sample = 10; // default sample interval for quantizer

    private boolean hasTransparentPixels;

    /**
     * Sets the delay time between each frame, or changes it for subsequent frames
     * (applies to last frame added).
     *
     * @param ms
     *          int delay time in milliseconds
     */
    public void setDelay(int ms) {
        delay = Math.round(ms / 10.0f);
    }

    /**
     * Sets the GIF frame disposal code for the last added frame and any
     * subsequent frames. Default is 0 if no transparent color has been set,
     * otherwise 2.
     *
     * @param code
     *          int disposal code.
     */
    public void setDispose(int code) {
        if (code >= 0) {
            dispose = code;
        }
    }

    /**
     * Sets the number of times the set of GIF frames should be played. Default is
     * 1; 0 means play indefinitely. Must be invoked before the first image is
     * added.
     *
     * @param iter
     *          int number of iterations.
     */
    public void setRepeat(int iter) {
        if (iter >= 0) {
            repeat = iter;
        }
    }

    /**
     * Sets the transparent color for the last added frame and any subsequent
     * frames. Since all colors are subject to modification in the quantization
     * process, the color in the final palette for each frame closest to the given
     * color becomes the transparent color for that frame. May be set to null to
     * indicate no transparent color.
     *
     * @param color
     *          Color to be treated as transparent on display.
     */
    public void setTransparent(int color) {
        transparent = color;
    }

    /**
     * Adds next GIF frame. The frame is not written immediately, but is actually
     * deferred until the next frame is received so that timing data can be
     * inserted. Invoking <code>finish()</code> flushes all frames. If
     * <code>setSize</code> was invoked, the size is used for all subsequent frames.
     * Otherwise, the actual size of the image is used for each frames.
     *
     * @param im
     *          BufferedImage containing frame to write.
     * @return true if successful.
     */
    public boolean addFrame(@Nullable Bitmap im) {
        return addFrame(im, 0, 0);
    }

    /**
     * Adds next GIF frame to the specified position. The frame is not written immediately, but is
     * actually deferred until the next frame is received so that timing data can be inserted.
     * Invoking <code>finish()</code> flushes all frames. If <code>setSize</code> was invoked, the
     * size is used for all subsequent frames. Otherwise, the actual size of the image is used for
     * each frame.
     *
     * See page 11 of http://giflib.sourceforge.net/gif89.txt for the position of the frame
     *
     * @param im
     *          BufferedImage containing frame to write.
     * @param x
     *          Column number, in pixels, of the left edge of the image, with respect to the left
     *          edge of the Logical Screen.
     * @param y
     *          Row number, in pixels, of the top edge of the image with respect to the top edge of
     *          the Logical Screen.
     * @return true if successful.
     */
    public boolean addFrame(@Nullable Bitmap im, int x, int y) {
        if ((im == null) || !started) {
            return false;
        }
        boolean ok = true;
        try {
            if (sizeSet) {
                setFrameSize(fixedWidth, fixedHeight);
            } else {
                setFrameSize(im.getWidth(), im.getHeight());
            }
            image = im;
            getImagePixels(); // convert to correct format if necessary
            analyzePixels(); // build color table & map pixels
            if (firstFrame) {
                writeLSD(); // logical screen descriptor
                writePalette(); // global color table
                if (repeat >= 0) {
                    // use NS app extension to indicate reps
                    writeNetscapeExt();
                }
            }
            writeGraphicCtrlExt(); // write graphic control extension
            writeImageDesc(x, y); // image descriptor
            if (!firstFrame) {
                writePalette(); // local color table
            }
            writePixels(); // encode and write pixel data
            firstFrame = false;
        } catch (IOException e) {
            ok = false;
        }

        return ok;
    }

    /**
     * Flushes any pending data and closes output file. If writing to an
     * OutputStream, the stream is not closed.
     */
    public boolean finish() {
        if (!started)
            return false;
        boolean ok = true;
        started = false;
        try {
            out.write(0x3b); // GIF trailer
            out.flush();
            if (closeStream) {
                out.close();
            }
        } catch (IOException e) {
            ok = false;
        }

        // reset for subsequent use
        transIndex = 0;
        out = null;
        image = null;
        pixels = null;
        indexedPixels = null;
        colorTab = null;
        closeStream = false;
        firstFrame = true;

        return ok;
    }

    /**
     * Sets frame rate in frames per second. Equivalent to
     * <code>setDelay(1000/fps)</code>.
     *
     * @param fps
     *          float frame rate (frames per second)
     */
    public void setFrameRate(float fps) {
        if (fps != 0f) {
            delay = Math.round(100f / fps);
        }
    }

    /**
     * Sets quality of color quantization (conversion of images to the maximum 256
     * colors allowed by the GIF specification). Lower values (minimum = 1)
     * produce better colors, but slow processing significantly. 10 is the
     * default, and produces good color mapping at reasonable speeds. Values
     * greater than 20 do not yield significant improvements in speed.
     *
     * @param quality int greater than 0.
     */
    public void setQuality(int quality) {
        if (quality < 1)
            quality = 1;
        sample = quality;
    }

    /**
     * Sets the fixed GIF frame size for all the frames.
     * This should be called before start.
     *
     * @param w
     *          int frame width.
     * @param h
     *          int frame width.
     */
    public void setSize(int w, int h) {
        if (started) {
            return;
        }

        fixedWidth = w;
        fixedHeight = h;
        if (fixedWidth < 1) {
            fixedWidth = 320;
        }
        if (fixedHeight < 1) {
            fixedHeight = 240;
        }

        sizeSet = true;
    }

    /**
     * Sets current GIF frame size.
     *
     * @param w
     *          int frame width.
     * @param h
     *          int frame width.
     */
    private void setFrameSize(int w, int h) {
        width = w;
        height = h;
    }

    /**
     * Initiates GIF file creation on the given stream. The stream is not closed
     * automatically.
     *
     * @param os
     *          OutputStream on which GIF images are written.
     * @return false if initial write failed.
     */
    public boolean start(@Nullable OutputStream os) {
        if (os == null)
            return false;
        boolean ok = true;
        closeStream = false;
        out = os;
        try {
            writeString("GIF89a"); // header
        } catch (IOException e) {
            ok = false;
        }
        return started = ok;
    }

    /**
     * Initiates writing of a GIF file with the specified name.
     *
     * @param file
     *          String containing output file name.
     * @return false if open or initial write failed.
     */
    public boolean start(@NonNull String file) {
        boolean ok;
        try {
            out = new BufferedOutputStream(new FileOutputStream(file));
            ok = start(out);
            closeStream = true;
        } catch (IOException e) {
            ok = false;
        }
        return started = ok;
    }

    /**
     * Analyzes image colors and creates color map.
     */
    private void analyzePixels() {
        int len = pixels.length;
        int nPix = len / 3;
        indexedPixels = new byte[nPix];
        NeuQuant nq = new NeuQuant(pixels, len, sample);
        // initialize quantizer
        colorTab = nq.process(); // create reduced palette
        // convert map from BGR to RGB
        for (int i = 0; i < colorTab.length; i += 3) {
            byte temp = colorTab[i];
            colorTab[i] = colorTab[i + 2];
            colorTab[i + 2] = temp;
            usedEntry[i / 3] = false;
        }
        // map image pixels to new palette
        int k = 0;
        for (int i = 0; i < nPix; i++) {
            int index = nq.map(pixels[k++] & 0xff, pixels[k++] & 0xff, pixels[k++] & 0xff);
            usedEntry[index] = true;
            indexedPixels[i] = (byte) index;
        }
        pixels = null;
        colorDepth = 8;
        palSize = 7;
        // get closest match to transparent color if specified
        if (transparent != null) {
            transIndex = findClosest(transparent);
        } else if (hasTransparentPixels) {
            transIndex = findClosest(Color.TRANSPARENT);
        }
    }

    /**
     * Returns index of palette color closest to c
     *
     */
    private int findClosest(int color) {
        if (colorTab == null)
            return -1;
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        int minpos = 0;
        int dmin = 256 * 256 * 256;
        int len = colorTab.length;
        for (int i = 0; i < len;) {
            int dr = r - (colorTab[i++] & 0xff);
            int dg = g - (colorTab[i++] & 0xff);
            int db = b - (colorTab[i] & 0xff);
            int d = dr * dr + dg * dg + db * db;
            int index = i / 3;
            if (usedEntry[index] && (d < dmin)) {
                dmin = d;
                minpos = index;
            }
            i++;
        }
        return minpos;
    }

    /**
     * Extracts image pixels into byte array "pixels"
     */
    private void getImagePixels() {
        int w = image.getWidth();
        int h = image.getHeight();

        if ((w != width) || (h != height)) {
            // create new image with right size/format
            Bitmap temp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(temp);
            canvas.drawBitmap(temp, 0, 0, null);
            image = temp;
        }
        int[] pixelsInt = new int[w * h];
        image.getPixels(pixelsInt, 0, w, 0, 0, w, h);

        // The algorithm requires 3 bytes per pixel as RGB.
        pixels = new byte[pixelsInt.length * 3];

        int pixelsIndex = 0;
        hasTransparentPixels = false;
        int totalTransparentPixels = 0;
        for (final int pixel : pixelsInt) {
            if (pixel == Color.TRANSPARENT) {
                totalTransparentPixels++;
            }
            pixels[pixelsIndex++] = (byte) (pixel & 0xFF);
            pixels[pixelsIndex++] = (byte) ((pixel >> 8) & 0xFF);
            pixels[pixelsIndex++] = (byte) ((pixel >> 16) & 0xFF);
        }

        double transparentPercentage = 100 * totalTransparentPixels / (double) pixelsInt.length;
        // Assume images with greater where more than n% of the pixels are transparent actually have
        // transparency. See issue #214.
        hasTransparentPixels = transparentPercentage > MIN_TRANSPARENT_PERCENTAGE;
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "got pixels for frame with " + transparentPercentage
                + "% transparent pixels");
        }
    }

    /**
     * Writes Graphic Control Extension
     */
    private void writeGraphicCtrlExt() throws IOException {
        out.write(0x21); // extension introducer
        out.write(0xf9); // GCE label
        out.write(4); // data block size
        int transp, disp;
        if (transparent == null && !hasTransparentPixels) {
            transp = 0;
            disp = 0; // dispose = no action
        } else {
            transp = 1;
            disp = 2; // force clear if using transparent color
        }
        if (dispose >= 0) {
            disp = dispose & 7; // user override
        }
        disp <<= 2;

        // packed fields
        out.write(0 | // 1:3 reserved
                disp | // 4:6 disposal
                0 | // 7 user input - 0 = none
                transp); // 8 transparency flag

        writeShort(delay); // delay x 1/100 sec
        out.write(transIndex); // transparent color index
        out.write(0); // block terminator
    }

    /**
     * Writes Image Descriptor
     */
    private void writeImageDesc(int x, int y) throws IOException {
        out.write(0x2c); // image separator
        writeShort(x); // image position
        writeShort(y);
        writeShort(width); // image size
        writeShort(height);
        // packed fields
        if (firstFrame) {
            // no LCT - GCT is used for first (or only) frame
            out.write(0);
        } else {
            // specify normal LCT
            out.write(0x80 | // 1 local color table 1=yes
                    0 | // 2 interlace - 0=no
                    0 | // 3 sorted - 0=no
                    0 | // 4-5 reserved
                    palSize); // 6-8 size of color table
        }
    }

    /**
     * Writes Logical Screen Descriptor
     */
    private void writeLSD() throws IOException {
        // logical screen size
        writeShort(width);
        writeShort(height);
        // packed fields
        out.write((0x80 | // 1 : global color table flag = 1 (gct used)
                0x70 | // 2-4 : color resolution = 7
                0x00 | // 5 : gct sort flag = 0
                palSize)); // 6-8 : gct size

        out.write(0); // background color index
        out.write(0); // pixel aspect ratio - assume 1:1
    }

    /**
     * Writes Netscape application extension to define repeat count.
     */
    private void writeNetscapeExt() throws IOException {
        out.write(0x21); // extension introducer
        out.write(0xff); // app extension label
        out.write(11); // block size
        writeString("NETSCAPE" + "2.0"); // app id + auth code
        out.write(3); // sub-block size
        out.write(1); // loop sub-block id
        writeShort(repeat); // loop count (extra iterations, 0=repeat forever)
        out.write(0); // block terminator
    }

    /**
     * Writes color table
     */
    private void writePalette() throws IOException {
        out.write(colorTab, 0, colorTab.length);
        int n = (3 * 256) - colorTab.length;
        for (int i = 0; i < n; i++) {
            out.write(0);
        }
    }

    /**
     * Encodes and writes pixel data
     */
    private void writePixels() throws IOException {
        LZWEncoder encoder = new LZWEncoder(width, height, indexedPixels, colorDepth);
        encoder.encode(out);
    }

    /**
     * Write 16-bit value to output stream, LSB first
     */
    private void writeShort(int value) throws IOException {
        out.write(value & 0xff);
        out.write((value >> 8) & 0xff);
    }

    /**
     * Writes string to output stream
     */
    private void writeString(String s) throws IOException {
        for (int i = 0; i < s.length(); i++) {
            out.write((byte) s.charAt(i));
        }
    }
}
