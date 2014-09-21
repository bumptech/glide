package com.bumptech.glide.load.engine.bitmap_recycle;

import android.graphics.Bitmap;

/**
 * An {@link com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool BitmapPool} implementation that rejects all
 * {@link android.graphics.Bitmap Bitmap}s added to it and always returns {@code null} from get.
 */
public class BitmapPoolAdapter implements BitmapPool {
    @Override
    public int getMaxSize() {
        return 0;
    }

    @Override
    public void setSizeMultiplier(float sizeMultiplier) {
        // Do nothing.
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
    public Bitmap getDirty(int width, int height, Bitmap.Config config) {
        return null;
    }

    @Override
    public void clearMemory() {
        // Do nothing.
    }

    @Override
    public void trimMemory(int level) {
        // Do nothing.
    }
}
