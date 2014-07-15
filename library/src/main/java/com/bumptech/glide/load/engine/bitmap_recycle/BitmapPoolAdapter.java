package com.bumptech.glide.load.engine.bitmap_recycle;

import android.graphics.Bitmap;

/**
 * An {@link com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool} implementation that rejects all
 * {@link android.graphics.Bitmap}s added to it and always returns {@code null} from get.
 */
public class BitmapPoolAdapter implements BitmapPool {
    @Override
    public void setSizeMultiplier(float sizeMultiplier) {
    }

    @Override
    public boolean put(Bitmap bitmap) {
        return false;
    }

    @Override
    public Bitmap get(int width, int height, Bitmap.Config config) {
        return null;
    }

    @Override
    public void clearMemory() { }

    @Override
    public void trimMemory(int level) { }
}
