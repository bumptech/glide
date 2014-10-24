package com.bumptech.glide.load.engine.bitmap_recycle;

import android.graphics.Bitmap;

/**
 * An interface for a pool that allows users to reuse {@link android.graphics.Bitmap} objects.
 */
public interface BitmapPool {

    /**
     * Returns the current maximum size of the pool in bytes.
     */
    int getMaxSize();

    /**
     * Multiplies the initial size of the pool by the given multipler to dynamically and synchronously allow users to
     * adjust the size of the pool.
     *
     * <p>
     *     If the current total size of the pool is larger than the max size after the given multiplier is applied,
     *     {@link Bitmap}s should be evicted until the pool is smaller than the new max size.
     * </p>
     *
     * @param sizeMultiplier The size multiplier to apply between 0 and 1.
     */
    void setSizeMultiplier(float sizeMultiplier);

    /**
     * Adds the given {@link android.graphics.Bitmap} and returns {@code true} if the {@link android.graphics.Bitmap}
     * was eligible to be added and {@code false} otherwise.
     *
     * <p>
     *     Note - If the {@link android.graphics.Bitmap} is rejected (this method returns false) then it is the caller's
     *     responsibility to call {@link android.graphics.Bitmap#recycle()}.
     * </p>
     *
     * <p>
     *     Note - This method will return {@code true} if the given {@link android.graphics.Bitmap} is synchronously
     *     evicted after being accepted. The only time this method will return {@code false} is if the
     *     {@link android.graphics.Bitmap} is not eligible to be added to the pool (either it is not mutable or it is
     *     larger than the max pool size).
     * </p>
     *
     * @see android.graphics.Bitmap#isMutable()
     * @see android.graphics.Bitmap#recycle()
     *
     * @param bitmap The {@link android.graphics.Bitmap} to attempt to add.
     */
    boolean put(Bitmap bitmap);

    /**
     * Returns a {@link android.graphics.Bitmap} of exactly the given width, height, and configuration, and containing
     * only transparent pixels or null if no such {@link android.graphics.Bitmap} could be obtained from the pool.
     *
     * <p>
     *     Because this method erases all pixels in the {@link Bitmap}, this method is slightly slower than
     *     {@link #getDirty(int, int, android.graphics.Bitmap.Config)}. If the {@link android.graphics.Bitmap} is being
     *     obtained to be used in {@link android.graphics.BitmapFactory} or in any other case where every pixel in the
     *     {@link android.graphics.Bitmap} will always be overwritten or cleared,
     *     {@link #getDirty(int, int, android.graphics.Bitmap.Config)} will be faster. When in doubt, use this method
     *     to ensure correctness.
     * </p>
     *
     * <pre>
     *     Implementations can should clear out every returned Bitmap using the following:
     *
     * {@code
     * bitmap.eraseColor(Color.TRANSPARENT);
     * }
     * </pre>
     *
     * @see #getDirty(int, int, android.graphics.Bitmap.Config)
     *
     * @param width The width in pixels of the desired {@link android.graphics.Bitmap}.
     * @param height The height in pixels of the desired {@link android.graphics.Bitmap}.
     * @param config The {@link android.graphics.Bitmap.Config} of the desired {@link android.graphics.Bitmap}.
     */
    Bitmap get(int width, int height, Bitmap.Config config);

    /**
     * Identical to {@link #get(int, int, android.graphics.Bitmap.Config)} except that any returned non-null
     * {@link android.graphics.Bitmap} may <em>not</em> have been erased and may contain random data.
     *
     * <p>
     *     Although this method is slightly more efficient than {@link #get(int, int, android.graphics.Bitmap.Config)}
     *     it should be used with caution and only when the caller is sure that they are going to erase the
     *     {@link android.graphics.Bitmap} entirely before writing new data to it.
     * </p>
     *
     * @see #get(int, int, android.graphics.Bitmap.Config)
     *
     * @param width The width in pixels of the desired {@link android.graphics.Bitmap}.
     * @param height The height in pixels of the desired {@link android.graphics.Bitmap}.
     * @param config The {@link android.graphics.Bitmap.Config} of the desired {@link android.graphics.Bitmap}.
     * @return A {@link android.graphics.Bitmap} with exactly the given width, height, and config potentially containing
     * random image data or null if no such {@link android.graphics.Bitmap} could be obtained from the pool.
     */
    Bitmap getDirty(int width, int height, Bitmap.Config config);

    /**
     * Removes all {@link android.graphics.Bitmap}s from the pool.
     */
    void clearMemory();

    /**
     * Reduces the size of the cache by evicting items based on the given level.
     *
     * @see android.content.ComponentCallbacks2
     *
     * @param level The level from {@link android.content.ComponentCallbacks2} to use to determine how many
     * {@link android.graphics.Bitmap}s to evict.
     */
    void trimMemory(int level);
}
