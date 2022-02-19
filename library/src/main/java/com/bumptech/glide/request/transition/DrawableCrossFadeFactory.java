package com.bumptech.glide.request.transition;

import android.graphics.drawable.Drawable;
import com.bumptech.glide.load.DataSource;

/**
 * A factory class that produces a new {@link Transition} that varies depending on whether or not
 * the drawable was loaded from the memory cache and whether or not the drawable is the first image
 * to be put on the target.
 *
 * <p>Resources are usually loaded from the memory cache just before the user can see the view, for
 * example when the user changes screens or scrolls back and forth in a list. In those cases the
 * user typically does not expect to see a transition. As a result, when the resource is loaded from
 * the memory cache this factory produces an {@link NoTransition}.
 */
// Public API.
@SuppressWarnings("WeakerAccess")
public class DrawableCrossFadeFactory implements TransitionFactory<Drawable> {
  private final int duration;
  private final boolean isCrossFadeEnabled;
  private DrawableCrossFadeTransition resourceTransition;

  protected DrawableCrossFadeFactory(int duration, boolean isCrossFadeEnabled) {
    this.duration = duration;
    this.isCrossFadeEnabled = isCrossFadeEnabled;
  }

  @Override
  public Transition<Drawable> build(DataSource dataSource, boolean isFirstResource) {
    return dataSource == DataSource.MEMORY_CACHE
        ? NoTransition.<Drawable>get()
        : getResourceTransition();
  }

  private Transition<Drawable> getResourceTransition() {
    if (resourceTransition == null) {
      resourceTransition = new DrawableCrossFadeTransition(duration, isCrossFadeEnabled);
    }
    return resourceTransition;
  }

  /** A Builder for {@link DrawableCrossFadeFactory}. */
  @SuppressWarnings("unused")
  public static class Builder {
    private static final int DEFAULT_DURATION_MS = 300;
    private final int durationMillis;
    private boolean isCrossFadeEnabled;

    public Builder() {
      this(DEFAULT_DURATION_MS);
    }

    /**
     * @param durationMillis The duration of the cross fade animation in milliseconds.
     */
    public Builder(int durationMillis) {
      this.durationMillis = durationMillis;
    }

    /**
     * Enables or disables animating the alpha of the {@link Drawable} the cross fade will animate
     * from.
     *
     * <p>Defaults to {@code false}.
     *
     * @param isCrossFadeEnabled If {@code true} the previous {@link Drawable}'s alpha will be
     *     animated from 100 to 0 while the new {@link Drawable}'s alpha is animated from 0 to 100.
     *     Otherwise the previous {@link Drawable}'s alpha will remain at 100 throughout the
     *     animation. See {@link
     *     android.graphics.drawable.TransitionDrawable#setCrossFadeEnabled(boolean)}
     */
    public Builder setCrossFadeEnabled(boolean isCrossFadeEnabled) {
      this.isCrossFadeEnabled = isCrossFadeEnabled;
      return this;
    }

    public DrawableCrossFadeFactory build() {
      return new DrawableCrossFadeFactory(durationMillis, isCrossFadeEnabled);
    }
  }
}
