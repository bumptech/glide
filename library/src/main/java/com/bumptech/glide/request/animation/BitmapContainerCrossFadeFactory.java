package com.bumptech.glide.request.animation;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.animation.Animation;

/**
 * A cross fade {@link GlideAnimation} for complex types that have a {@link android.graphics.Bitmap} inside
 * that uses an {@link android.graphics.drawable.TransitionDrawable} to transition from an existing drawable
 * already visible on the target to the new bitmap. If no existing drawable exists, this class can
 * instead fall back to a default animation that doesn't rely on {@link android.graphics.drawable.TransitionDrawable}.
 * The new bitmap queried from the complex type is wrapped in a {@link android.graphics.drawable.BitmapDrawable}.
 *
 * @param <T> The type of the composite object that contains the {@link android.graphics.Bitmap} to be animated.
 */
public abstract class BitmapContainerCrossFadeFactory<T> implements GlideAnimationFactory<T> {
    private final GlideAnimationFactory<Drawable> realFactory;

    public BitmapContainerCrossFadeFactory() {
        this(new DrawableCrossFadeFactory<Drawable>());
    }

    public BitmapContainerCrossFadeFactory(int duration) {
        this(new DrawableCrossFadeFactory<Drawable>(duration));
    }

    public BitmapContainerCrossFadeFactory(Context context, int defaultAnimationId, int duration) {
        this(new DrawableCrossFadeFactory<Drawable>(context, defaultAnimationId, duration));
    }

    public BitmapContainerCrossFadeFactory(Animation defaultAnimation, int duration) {
        this(new DrawableCrossFadeFactory<Drawable>(defaultAnimation, duration));
    }

    public BitmapContainerCrossFadeFactory(GlideAnimationFactory<Drawable> realFactory) {
        this.realFactory = realFactory;
    }

    @Override
    public GlideAnimation<T> build(boolean isFromMemoryCache, boolean isFirstResource) {
        GlideAnimation<Drawable> transition = realFactory.build(isFromMemoryCache, isFirstResource);
        return new BitmapGlideAnimation(transition);
    }

    /**
     * Retrieve the Bitmap from a composite object.
     * <br>
     * <b>Warning:</b> Do not convert any arbitrary object to Bitmap via expensive drawing here.
     *
     * @param current composite object containing a Bitmap and some other information
     * @return the Bitmap contained within {@code current}
     */
    protected abstract Bitmap getBitmap(T current);

    private class BitmapGlideAnimation implements GlideAnimation<T> {
        private final GlideAnimation<Drawable> transition;

        public BitmapGlideAnimation(GlideAnimation<Drawable> transition) {
            this.transition = transition;
        }

        @Override
        public boolean animate(T current, ViewAdapter adapter) {
            Resources resources = adapter.getView().getResources();
            Drawable currentBitmap = new BitmapDrawable(resources, getBitmap(current));
            return transition.animate(currentBitmap, adapter);
        }
    }
}
