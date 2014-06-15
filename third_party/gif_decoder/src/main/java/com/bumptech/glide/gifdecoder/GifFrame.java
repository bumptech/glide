package com.bumptech.glide.gifdecoder;

/**
 * Inner model class housing metadata for each frame
 */
class GifFrame {
    public int ix, iy, iw, ih;
    /* Control Flags */
    public boolean interlace;
    public boolean transparency;
    /* Disposal Method */
    public int dispose;
    /* Transparency Index */
    public int transIndex;
    /* Delay, in ms, to next frame */
    public int delay;
    /* Index in the raw buffer where we need to start reading to decode */
    public int bufferFrameStart;
    /* Local Color Table */
    public int[] lct;
}
