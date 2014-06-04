package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import com.bumptech.glide.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.util.Util;

public class BitmapResource extends Resource<Bitmap> {
    private Bitmap bitmap;
    private BitmapPool bitmapPool;

    public BitmapResource(Bitmap bitmap, BitmapPool bitmapPool) {
        this.bitmap = bitmap;
        this.bitmapPool = bitmapPool;
    }

    @Override
    public Bitmap get() {
        return bitmap;
    }

    @Override
    public int getSize() {
        return Util.getSize(bitmap);
    }

    @Override
    public void recycleInternal() {
        if (!bitmapPool.put(bitmap)) {
            bitmap.recycle();
        }
    }
}
