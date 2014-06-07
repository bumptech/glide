package com.bumptech.glide.load.resource.drawable;

import android.graphics.Bitmap;
import com.bumptech.glide.Resource;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.resource.gif.GifDrawable;

import java.io.OutputStream;

public class GifBitmapResourceEncoder implements ResourceEncoder<GifBitmap> {
    private final ResourceEncoder<Bitmap> bitmapEncoder;
    private final ResourceEncoder<GifDrawable> gifEncoder;

    public GifBitmapResourceEncoder(ResourceEncoder<Bitmap> bitmapEncoder, ResourceEncoder<GifDrawable> gifEncoder) {
        this.bitmapEncoder = bitmapEncoder;
        this.gifEncoder = gifEncoder;
    }

    @Override
    public boolean encode(Resource<GifBitmap> resource, OutputStream os) {
        final GifBitmap gifBitmap = resource.get();
        final Resource<Bitmap> bitmapResource = gifBitmap.getBitmapResource();

        if (bitmapResource != null) {
            return bitmapEncoder.encode(bitmapResource, os);
        } else {
            return gifEncoder.encode(gifBitmap.getGifResource(), os);
        }
    }

    @Override
    public String getId() {
        return "GifBitmapResourceEncoder.com.bumptech.glide.load.resource.drawable";
    }
}
