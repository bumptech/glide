package com.bumptech.glide.load.resource.drawable;

import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;

/**
 * A base class for drawables that are either static equivalents of {@link android.graphics.drawable.BitmapDrawable} or
 * that contain an animation.
 */
public abstract class GlideDrawable extends Drawable implements Animatable {
    /** A constant indicating that an animated drawable should loop continuously. */
    public static final int LOOP_FOREVER = -1;
    /**
     * A constant indicating that an animated drawable should loop for its default number of times. For animated GIFs,
     * this constant indicates the GIF should use the netscape loop count if present.
     */
    public static final int LOOP_INTRINSIC = 0;

    /**
     * Returns {@code true} if this drawable is animated.
     */
    public abstract boolean isAnimated();

    /**
     * Sets the number of times the animation should loop. This method will only have an affect if
     * {@link #isAnimated ()}}  returns {@code true}. A loop count of <=0 indicates loop forever.
     */
    public abstract void setLoopCount(int loopCount);
}
