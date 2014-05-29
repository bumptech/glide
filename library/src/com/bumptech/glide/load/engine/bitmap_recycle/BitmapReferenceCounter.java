package com.bumptech.glide.load.engine.bitmap_recycle;

import android.graphics.Bitmap;

public interface BitmapReferenceCounter {

    public void acquireBitmap(Bitmap bitmap);

    public void releaseBitmap(Bitmap bitmap);
}
