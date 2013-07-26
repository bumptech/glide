package com.bumptech.glide.resize.bitmap_recycle;

import android.graphics.Bitmap;

public interface BitmapReferenceCounter {

    public void initBitmap(Bitmap bitmap);

    public void acquireBitmap(Bitmap bitmap);

    public void releaseBitmap(Bitmap bitmap);

    public void rejectBitmap(Bitmap bitmap);

    /**
     * A method notifying the tracker that this bitmap is referenced but not necessarily used
     * by an external object. These bitmaps will not be recycled if their references drop to 0 unless they are
     * first accepted or are rejected before or after their references drop to 0. This is used because the memory cache
     * can force a bitmap to be removed b/c of size constraints while a callback referencing that bitmap is still
     * on the queue of the main thread waiting to be called. If the bitmap were not marked and the memory cache released
     * the bitmap before the callback was called on the main thread, then the bitmap would be placed in the queue to be
     * recycled once by the memory cache and then again by the object owning the callback.
     *
     * @param bitmap The bitmap to mark
     */
     public void markPending(Bitmap bitmap);
}
