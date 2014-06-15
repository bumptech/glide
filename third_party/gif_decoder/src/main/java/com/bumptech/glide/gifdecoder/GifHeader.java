package com.bumptech.glide.gifdecoder;

import java.util.ArrayList;
import java.util.List;

public class GifHeader {

    public int[] gct = null;
    /**
     * Global status code of GIF data parsing
     */
    public int status = GifDecoder.STATUS_OK;
    public int frameCount = 0;

    public GifFrame currentFrame;
    public List<GifFrame> frames = new ArrayList<GifFrame>();
     // logical screen size
    public int width; // full image width
    public int height; // full image height

    public boolean gctFlag; // 1 : global color table flag
    // 2-4 : color resolution
    // 5 : gct sort flag
    public int gctSize; // 6-8 : gct size
    public int bgIndex; // background color index
    public int pixelAspect; // pixel aspect ratio
    //TODO: this is set both during reading the header and while decoding frames...
    public int bgColor;
    public boolean isTransparent;
    public int loopCount;
}
