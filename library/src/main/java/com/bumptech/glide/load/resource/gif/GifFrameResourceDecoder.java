package com.bumptech.glide.load.resource.gif;

import android.graphics.Bitmap;
import com.bumptech.glide.Resource;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.resource.gif.decoder.GifDecoder;

import java.io.IOException;

class GifFrameResourceDecoder implements ResourceDecoder<GifDecoder, Bitmap> {

    @Override
    public Resource<Bitmap> decode(GifDecoder source, int width, int height) throws IOException {
        return source.getNextFrame();
    }

    @Override
    public String getId() {
        return "GifFrameResourceDecoder.com.bumptech.glide.load.resource.gif";
    }
}
