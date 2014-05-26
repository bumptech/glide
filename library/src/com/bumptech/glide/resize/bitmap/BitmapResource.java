package com.bumptech.glide.resize.bitmap;

import android.graphics.Bitmap;
import com.bumptech.glide.resize.Resource;
import com.bumptech.glide.util.Util;

public class BitmapResource implements Resource<Bitmap> {
    private Bitmap bitmap;

    public BitmapResource(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    @Override
    public Bitmap get() {
        return bitmap;
    }

    @Override
    public int getSize() {
        return Util.getSize(bitmap);
    }
}
