package com.bumptech.glide.load.engine.bitmap_recycle;

import android.graphics.Bitmap;

interface LruPoolStrategy {
    public void put(Bitmap bitmap);
    public Bitmap get(int width, int height, Bitmap.Config config);
    public Bitmap removeLast();
    public String logBitmap(Bitmap bitmap);
    public String logBitmap(int width, int height, Bitmap.Config config);
    public int getSize(Bitmap bitmap);
}
