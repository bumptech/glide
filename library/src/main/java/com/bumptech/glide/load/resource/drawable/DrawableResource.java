package com.bumptech.glide.load.resource.drawable;

import android.graphics.drawable.Drawable;

import com.bumptech.glide.load.engine.Resource;

/**
 * Simple wrapper for an Android {@link Drawable} which returns a
 * {@link android.graphics.drawable.Drawable.ConstantState#newDrawable() new drawable}
 * based on it's {@link android.graphics.drawable.Drawable.ConstantState state}.
 *
 * <b>Suggested usages only include {@code T}s where the new drawable is of the same or descendant class.</b>
 *
 * @param <T> type of the wrapped {@link Drawable}
 */
public abstract class DrawableResource<T extends Drawable> implements Resource<T> {
    protected final T drawable;
    private boolean returnedOriginalDrawable;

    public DrawableResource(T drawable) {
        if (drawable == null) {
            throw new NullPointerException("Drawable must not be null!");
        }
        this.drawable = drawable;
    }

    @SuppressWarnings("unchecked")
    // drawables should always return a copy of the same class
    @Override
    public final T get() {
        if (!returnedOriginalDrawable) {
            returnedOriginalDrawable = true;
            return drawable;
        } else {
            return (T) drawable.getConstantState().newDrawable();
        }
    }
}
