package com.bumptech.glide.request.target;

import android.annotation.TargetApi;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * A wrapper drawable to square the wrapped drawable so that it expands to fill a square with exactly the given side
 * length. The goal of this drawable is to ensure that square thumbnail drawables always match the size of the view
 * they will be displayed in to avoid a costly requestLayout call. This class should not be used with views or drawables
 * that are not square.
 */
public class SquaringDrawable extends Drawable {
    private final Drawable wrapped;
    private int side;

    public SquaringDrawable(Drawable wrapped, int side) {
        this.wrapped = wrapped;
        this.side = side;
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);
        wrapped.setBounds(left, top, right, bottom);
    }

    @Override
    public void setBounds(Rect bounds) {
        super.setBounds(bounds);
        wrapped.setBounds(bounds);
    }
    public void setChangingConfigurations(int configs) {
        wrapped.setChangingConfigurations(configs);
    }

    @Override
    public int getChangingConfigurations() {
        return wrapped.getChangingConfigurations();
    }

    @Override
    public void setDither(boolean dither) {
        wrapped.setDither(dither);
    }

    @Override
    public void setFilterBitmap(boolean filter) {
        wrapped.setFilterBitmap(filter);
    }

    @TargetApi(11)
    @Override
    public Callback getCallback() {
        return wrapped.getCallback();
    }

    @TargetApi(19)
    @Override
    public int getAlpha() {
        return wrapped.getAlpha();
    }

    @Override
    public void setColorFilter(int color, PorterDuff.Mode mode) {
        wrapped.setColorFilter(color, mode);
    }

    @Override
    public void clearColorFilter() {
        wrapped.clearColorFilter();
    }

    @Override
    public Drawable getCurrent() {
        return wrapped.getCurrent();
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        return wrapped.setVisible(visible, restart);
    }

    @Override
    public int getIntrinsicWidth() {
        return side;
    }

    @Override
    public int getIntrinsicHeight() {
        return side;
    }

    @Override
    public int getMinimumWidth() {
        return wrapped.getMinimumWidth();
    }

    @Override
    public int getMinimumHeight() {
        return wrapped.getMinimumHeight();
    }

    @Override
    public boolean getPadding(Rect padding) {
        return wrapped.getPadding(padding);
    }

    @Override
    public void invalidateSelf() {
        super.invalidateSelf();    //To change body of overridden methods use File | Settings | File Templates.
        wrapped.invalidateSelf();
    }

    @Override
    public void unscheduleSelf(Runnable what) {
        super.unscheduleSelf(what);    //To change body of overridden methods use File | Settings | File Templates.
        wrapped.unscheduleSelf(what);
    }

    @Override
    public void scheduleSelf(Runnable what, long when) {
        super.scheduleSelf(what, when);    //To change body of overridden methods use File | Settings | File Templates.
        wrapped.scheduleSelf(what, when);
    }

    @Override
    public void draw(Canvas canvas) {
        wrapped.draw(canvas);
    }

    @Override
    public void setAlpha(int i) {
        wrapped.setAlpha(i);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        wrapped.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return wrapped.getOpacity();
    }
}
