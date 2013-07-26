package com.bumptech.glide.resize.bitmap_recycle;

import android.graphics.Bitmap;

public interface BitmapPool {
    public boolean put(Bitmap bitmap);
    public Bitmap get(int width, int height);
}
