package com.bumptech.glide.integration.palette;

import android.graphics.Bitmap;
import android.support.v7.graphics.Palette;

/**
 * A simple wrapper for a {@link android.support.v7.graphics.Palette} and a {@link android.graphics.Bitmap}.
 */
public class PaletteBitmap {
    public final Palette palette;
    public final Bitmap bitmap;

    public PaletteBitmap(Bitmap bitmap, Palette palette) {
        this.bitmap = bitmap;
        this.palette = palette;
    }
}
