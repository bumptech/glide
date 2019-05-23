package com.bumptech.glide.request.transition;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;

/**
 * A cross fade {@link Transition} for {@link android.graphics.drawable.Drawable}s that uses an
 * {@link android.graphics.drawable.TransitionDrawable} to transition from an existing drawable
 * already visible on the target to a new drawable. If no existing drawable exists, this class can
 * instead fall back to a default animation that doesn't rely on {@link
 * android.graphics.drawable.TransitionDrawable}.
 */
public class DrawableCrossFadeTransition implements Transition<Drawable> {
  private final int duration;
  private final boolean isCrossFadeEnabled;

  /**
   * @param duration The duration that the cross fade animation should run if there is something to
   *     cross fade from when a new {@link android.graphics.drawable.Drawable} is put.
   * @param isCrossFadeEnabled If {@code true}, animates the previous resource's alpha to 0 while
   *     animating the new resource's alpha to 100. Otherwise, only animates the new resource's
   *     alpha to 100 while leaving the previous resource's alpha at 100. See {@link
   *     TransitionDrawable#setCrossFadeEnabled(boolean)}.
   */
  // Public API.
  @SuppressWarnings("WeakerAccess")
  public DrawableCrossFadeTransition(int duration, boolean isCrossFadeEnabled) {
    this.duration = duration;
    this.isCrossFadeEnabled = isCrossFadeEnabled;
  }

  /**
   * Animates from the previous drawable to the current drawable in one of two ways.
   *
   * <ol>
   *   <li>Using the default animation provided in the constructor if the previous drawable is null
   *   <li>Using the cross fade animation with the duration provided in the constructor if the
   *       previous drawable is non null
   * </ol>
   *
   * @param current {@inheritDoc}
   * @param adapter {@inheritDoc}
   * @return {@inheritDoc}
   */
  @Override
  public boolean transition(Drawable current, ViewAdapter adapter) {
    Drawable previous = adapter.getCurrentDrawable();
    if (previous == null) {
      previous = new ColorDrawable(Color.TRANSPARENT);
    }
    TransitionDrawable transitionDrawable =
        new TransitionDrawable(new Drawable[] {previous, current});
    transitionDrawable.setCrossFadeEnabled(isCrossFadeEnabled);
    transitionDrawable.startTransition(duration);
    adapter.setDrawable(transitionDrawable);
    return true;
  }
}
