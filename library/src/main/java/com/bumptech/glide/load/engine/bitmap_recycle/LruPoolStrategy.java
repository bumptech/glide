package com.bumptech.glide.load.engine.bitmap_recycle;

import android.graphics.Bitmap;

interface LruPoolStrategy {
    void put(Bitmap bitmap);
    Bitmap get(int width, int height, Bitmap.Config config);
    Bitmap removeLast();
    String logBitmap(Bitmap bitmap);
    String logBitmap(int width, int height, Bitmap.Config config);
    int getSize(Bitmap bitmap);
}
