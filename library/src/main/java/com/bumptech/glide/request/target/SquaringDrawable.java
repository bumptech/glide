package com.bumptech.glide.request.target;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;

import com.bumptech.glide.load.resource.drawable.GlideDrawable;

/**
 * A wrapper drawable to square the wrapped drawable so that it expands to fill a square with exactly the given side
 * length. The goal of this drawable is to ensure that square thumbnail drawables always match the size of the view
 * they will be displayed in to avoid a costly requestLayout call. This class should not be used with views or drawables
 * that are not square.
 */
public class SquaringDrawable extends GlideDrawable {
    private GlideDrawable wrapped;
    private State state;
    private boolean mutated;

    public SquaringDrawable(GlideDrawable wrapped, int side) {
        this(new State(wrapped.getConstantState(), side), wrapped, null /*res*/);
    }

    SquaringDrawable(State state, GlideDrawable wrapped, Resources res) {
        this.state = state;
        if (wrapped == null) {
          if (res != null) {
            this.wrapped = (GlideDrawable) state.wrapped.newDrawable(res);
          } else {
            this.wrapped = (GlideDrawable) state.wrapped.newDrawable();
          }
        } else {
          this.wrapped = wrapped;
        }
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

    @Override
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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public Callback getCallback() {
        return wrapped.getCallback();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
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
        return state.side;
    }

    @Override
    public int getIntrinsicHeight() {
        return state.side;
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
        super.invalidateSelf();
        wrapped.invalidateSelf();
    }

    @Override
    public void unscheduleSelf(Runnable what) {
        super.unscheduleSelf(what);
        wrapped.unscheduleSelf(what);
    }

    @Override
    public void scheduleSelf(Runnable what, long when) {
        super.scheduleSelf(what, when);
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

    @Override
    public boolean isAnimated() {
        return wrapped.isAnimated();
    }

    @Override
    public void setLoopCount(int loopCount) {
        wrapped.setLoopCount(loopCount);
    }

    @Override
    public void start() {
        wrapped.start();
    }

    @Override
    public void stop() {
        wrapped.stop();
    }

    @Override
    public boolean isRunning() {
        return wrapped.isRunning();
    }

    @Override
    public Drawable mutate() {
        if (!mutated && super.mutate() == this) {
            wrapped = (GlideDrawable) wrapped.mutate();
            state = new State(state);
            mutated = true;
        }
        return this;
    }

    @Override
    public ConstantState getConstantState() {
        return state;
    }

    static class State extends ConstantState {
        private final ConstantState wrapped;
        private final int side;

        State(State other) {
          this(other.wrapped, other.side);
        }

        State(ConstantState wrapped, int side) {
          this.wrapped = wrapped;
          this.side = side;
        }

        @Override
        public Drawable newDrawable() {
          return newDrawable(null /*res*/);
        }

        @Override
        public Drawable newDrawable(Resources res) {
          return new SquaringDrawable(this, null /*wrapped*/, res);
        }

        @Override
        public int getChangingConfigurations() {
          return 0;
        }
    }
}
