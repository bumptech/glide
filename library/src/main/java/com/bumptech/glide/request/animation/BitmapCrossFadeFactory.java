package com.bumptech.glide.request.animation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.animation.Animation;

/**
 * A cross fade {@link GlideAnimation} for {@link android.graphics.Bitmap}s
 * that uses an {@link android.graphics.drawable.TransitionDrawable} to transition from an existing drawable
 * already visible on the target to the new bitmap. If no existing drawable exists, this class can
 * instead fall back to a default animation that doesn't rely on {@link android.graphics.drawable.TransitionDrawable}.
 * The new bitmap is wrapped in a {@link android.graphics.drawable.BitmapDrawable}.
 */
public class BitmapCrossFadeFactory extends BitmapContainerCrossFadeFactory<Bitmap> {
    public BitmapCrossFadeFactory() {
        super();
    }

    public BitmapCrossFadeFactory(int duration) {
        super(duration);
    }

    public BitmapCrossFadeFactory(Context context, int defaultAnimationId, int duration) {
        super(context, defaultAnimationId, duration);
    }

    public BitmapCrossFadeFactory(Animation defaultAnimation, int duration) {
        super(defaultAnimation, duration);
    }

    public BitmapCrossFadeFactory(GlideAnimationFactory<Drawable> realFactory) {
        super(realFactory);
    }

    @Override
    protected Bitmap getBitmap(Bitmap current) {
        return current;
    }
}
