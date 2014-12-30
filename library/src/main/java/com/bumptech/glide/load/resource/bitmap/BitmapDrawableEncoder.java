package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

import java.io.OutputStream;

public class BitmapDrawableEncoder implements ResourceEncoder<BitmapDrawable> {

    private final BitmapPool bitmapPool;
    private final ResourceEncoder<Bitmap> encoder;

    public BitmapDrawableEncoder(BitmapPool bitmapPool, ResourceEncoder<Bitmap> encoder) {
        this.bitmapPool = bitmapPool;
        this.encoder = encoder;
    }

    @Override
    public boolean encode(Resource<BitmapDrawable> data, OutputStream os) {
        return encoder.encode(new BitmapResource(data.get().getBitmap(), bitmapPool), os);
    }
}
