package com.bumptech.glide.load.resource.gif;

import com.bumptech.glide.Resource;

//TODO: make this safe for multiple consumers.
public class GifResource extends Resource<GifDrawable> {
    private final GifDrawable drawable;
    private final GifDecoder decoder;

    public GifResource(GifDecoder decoder, GifFrameManager frameManager) {
        this(decoder, new GifDrawable(decoder, frameManager));
    }

    GifResource(GifDecoder gifDecoder, GifDrawable gifDrawable) {
        decoder = gifDecoder;
        drawable = gifDrawable;
    }

    @Override
    public GifDrawable get() {
        return drawable;
    }

    @Override
    public int getSize() {
        return decoder.getGifByteSize();
    }

    @Override
    protected void recycleInternal() {
        drawable.stop();
        drawable.recycle();
    }
}
