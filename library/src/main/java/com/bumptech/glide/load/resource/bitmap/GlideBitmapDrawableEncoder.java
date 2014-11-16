package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;

import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

import java.io.OutputStream;

public class GlideBitmapDrawableEncoder implements ResourceEncoder<GlideBitmapDrawable> {

    private BitmapPool bitmapPool;
    private ResourceEncoder<Bitmap> encoder;

    public GlideBitmapDrawableEncoder(BitmapPool bitmapPool, ResourceEncoder<Bitmap> encoder) {
        this.bitmapPool = bitmapPool;
        this.encoder = encoder;
    }

    @Override
    public boolean encode(Resource<GlideBitmapDrawable> data, OutputStream os) {
        return encoder.encode(new BitmapResource(data.get().getBitmap(), bitmapPool), os);
    }

    @Override
    public String getId() {
        return encoder.getId();
    }
}
