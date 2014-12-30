package com.bumptech.glide.request.target;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

/**
 * A wrapper drawable to square the wrapped drawable so that it expands to fill a square with exactly the given side
 * length. The goal of this drawable is to ensure that square thumbnail drawables always match the size of the view
 * they will be displayed in to avoid a costly requestLayout call. This class should not be used with views or drawables
 * that are not square.
 */
public class FixedSizeDrawable extends LayerDrawable {
    private final int width;
    private final int height;

    /**
     * Create a new layer drawable with the list of specified layers.
     *
     * @param toWrap A list of drawables to use as layers in this new drawable.
     */
    public FixedSizeDrawable(Drawable toWrap, int width, int height) {
        super(new Drawable[] { toWrap });
        this.width = width;
        this.height = height;
    }

    @Override
    public int getIntrinsicWidth() {
        return width;
    }

    @Override
    public int getIntrinsicHeight() {
        return height;
    }
}
