package com.bumptech.glide.load.engine.bitmap_recycle;

import android.graphics.Bitmap;

public interface BitmapPool {
    public void setSizeMultiplier(float sizeMultiplier);

    public boolean put(Bitmap bitmap);

    public Bitmap get(int width, int height, Bitmap.Config config);

    public void clearMemory();

    public void trimMemory(int level);
}
