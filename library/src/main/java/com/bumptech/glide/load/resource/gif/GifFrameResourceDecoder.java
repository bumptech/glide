package com.bumptech.glide.load.resource.gif;

import android.graphics.Bitmap;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;

import java.io.IOException;

class GifFrameResourceDecoder implements ResourceDecoder<GifDecoder, Bitmap> {
    private BitmapPool bitmapPool;

    public GifFrameResourceDecoder(BitmapPool bitmapPool) {
        this.bitmapPool = bitmapPool;
    }

    @Override
    public Resource<Bitmap> decode(GifDecoder source, int width, int height) throws IOException {
        Bitmap bitmap = source.getNextFrame();
        return new BitmapResource(bitmap ,bitmapPool);
    }

    @Override
    public String getId() {
        return "GifFrameResourceDecoder.com.bumptech.glide.load.resource.gif";
    }
}
