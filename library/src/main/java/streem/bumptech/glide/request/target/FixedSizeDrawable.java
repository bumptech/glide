package com.bumptech.glide.request.target;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Synthetic;

/**
 * A wrapper drawable to square the wrapped drawable so that it expands to fill a square with
 * exactly the given side length. The goal of this drawable is to ensure that square thumbnail
 * drawables always match the size of the view they will be displayed in to avoid a costly
 * requestLayout call. This class should not be used with views or drawables that are not square.
 */
public class FixedSizeDrawable extends Drawable {
  private final Matrix matrix;
  private final RectF wrappedRect;
  private final RectF bounds;
  private Drawable wrapped;
  private State state;
  private boolean mutated;

  // Public API.
  @SuppressWarnings("WeakerAccess")
  public FixedSizeDrawable(Drawable wrapped, int width, int height) {
    this(new State(wrapped.getConstantState(), width, height), wrapped);
  }

  FixedSizeDrawable(State state, Drawable wrapped) {
    this.state = Preconditions.checkNotNull(state);
    this.wrapped = Preconditions.checkNotNull(wrapped);

    // We will do our own scaling.
    wrapped.setBounds(0, 0, wrapped.getIntrinsicWidth(), wrapped.getIntrinsicHeight());

    matrix = new Matrix();
    wrappedRect = new RectF(0, 0, wrapped.getIntrinsicWidth(), wrapped.getIntrinsicHeight());
    bounds = new RectF();
  }

  @Override
  public void setBounds(int left, int top, int right, int bottom) {
    super.setBounds(left, top, right, bottom);
    bounds.set(left, top, right, bottom);
    updateMatrix();
  }

  @Override
  public void setBounds(@NonNull Rect bounds) {
    super.setBounds(bounds);
    this.bounds.set(bounds);
    updateMatrix();
  }

  private void updateMatrix() {
    matrix.setRectToRect(wrappedRect, this.bounds, Matrix.ScaleToFit.CENTER);
  }

  @Override
  public void setChangingConfigurations(int configs) {
    wrapped.setChangingConfigurations(configs);
  }

  @Override
  public int getChangingConfigurations() {
    return wrapped.getChangingConfigurations();
  }

  @Deprecated
  @Override
  public void setDither(boolean dither) {
    wrapped.setDither(dither);
  }

  @Override
  public void setFilterBitmap(boolean filter) {
    wrapped.setFilterBitmap(filter);
  }

  @Override
  public Callback getCallback() {
    return wrapped.getCallback();
  }

  @RequiresApi(Build.VERSION_CODES.KITKAT)
  @Override
  public int getAlpha() {
    return wrapped.getAlpha();
  }

  @Override
  public void setColorFilter(int color, @NonNull PorterDuff.Mode mode) {
    wrapped.setColorFilter(color, mode);
  }

  @Override
  public void clearColorFilter() {
    wrapped.clearColorFilter();
  }

  @NonNull
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
    return state.width;
  }

  @Override
  public int getIntrinsicHeight() {
    return state.height;
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
  public boolean getPadding(@NonNull Rect padding) {
    return wrapped.getPadding(padding);
  }

  @Override
  public void invalidateSelf() {
    super.invalidateSelf();
    wrapped.invalidateSelf();
  }

  @Override
  public void unscheduleSelf(@NonNull Runnable what) {
    super.unscheduleSelf(what);
    wrapped.unscheduleSelf(what);
  }

  @Override
  public void scheduleSelf(@NonNull Runnable what, long when) {
    super.scheduleSelf(what, when);
    wrapped.scheduleSelf(what, when);
  }

  @Override
  public void draw(@NonNull Canvas canvas) {
    canvas.save();
    canvas.concat(matrix);
    wrapped.draw(canvas);
    canvas.restore();
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

  @NonNull
  @Override
  public Drawable mutate() {
    if (!mutated && super.mutate() == this) {
      wrapped = wrapped.mutate();
      state = new State(state);
      mutated = true;
    }
    return this;
  }

  @Override
  public ConstantState getConstantState() {
    return state;
  }

  static final class State extends ConstantState {
    private final ConstantState wrapped;
    @Synthetic final int width;
    @Synthetic final int height;

    State(State other) {
      this(other.wrapped, other.width, other.height);
    }

    State(ConstantState wrapped, int width, int height) {
      this.wrapped = wrapped;
      this.width = width;
      this.height = height;
    }

    @NonNull
    @Override
    public Drawable newDrawable() {
      return new FixedSizeDrawable(this, wrapped.newDrawable());
    }

    @NonNull
    @Override
    public Drawable newDrawable(Resources res) {
      return new FixedSizeDrawable(this, wrapped.newDrawable(res));
    }

    @Override
    public int getChangingConfigurations() {
      return 0;
    }
  }
}
