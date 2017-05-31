package com.bumptech.glide.request.transition;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import com.bumptech.glide.load.DataSource;

/**
 * A factory class that produces a new {@link Transition} that varies depending on whether or not
 * the drawable was loaded from the memory cache and whether or not the drawable is the first image
 * to be put on the target.
 *
 * <p> Resources are usually loaded from the memory cache just before the user can see the view, for
 * example when the user changes screens or scrolls back and forth in a list. In those cases the
 * user typically does not expect to see a transition. As a result, when the resource is loaded from
 * the memory cache this factory produces an {@link NoTransition}.
 */
public class DrawableCrossFadeFactory implements TransitionFactory<Drawable> {
  private final ViewAnimationFactory<Drawable> viewAnimationFactory;
  private final int duration;
  private final boolean isCrossFadeEnabled;
  private DrawableCrossFadeTransition firstResourceTransition;
  private DrawableCrossFadeTransition secondResourceTransition;

  protected DrawableCrossFadeFactory(ViewAnimationFactory<Drawable> viewAnimationFactory,
      int duration, boolean isCrossFadeEnabled) {
    this.viewAnimationFactory = viewAnimationFactory;
    this.duration = duration;
    this.isCrossFadeEnabled = isCrossFadeEnabled;
  }

  @Override
  public Transition<Drawable> build(DataSource dataSource, boolean isFirstResource) {
    if (dataSource == DataSource.MEMORY_CACHE) {
      return NoTransition.get();
    } else if (isFirstResource) {
      return getFirstResourceTransition(dataSource);
    } else {
      return getSecondResourceTransition(dataSource);
    }
  }

  private Transition<Drawable> getFirstResourceTransition(DataSource dataSource) {
      if (firstResourceTransition == null) {
        firstResourceTransition = buildTransition(dataSource, true /*isFirstResource*/);
      }
      return firstResourceTransition;
  }

  private Transition<Drawable> getSecondResourceTransition(DataSource dataSource) {
      if (secondResourceTransition == null) {
        secondResourceTransition = buildTransition(dataSource, false /*isFirstResource*/);
      }
      return secondResourceTransition;
  }

  private DrawableCrossFadeTransition buildTransition(DataSource dataSource,
      boolean isFirstResource) {
    Transition<Drawable> defaultAnimation =
        viewAnimationFactory.build(dataSource, isFirstResource);
    return new DrawableCrossFadeTransition(defaultAnimation, duration, isCrossFadeEnabled);
  }

  private static final class DefaultViewTransitionAnimationFactory implements
      ViewTransition.ViewTransitionAnimationFactory {

    private final int durationMillis;

    DefaultViewTransitionAnimationFactory(int durationMillis) {
      this.durationMillis = durationMillis;
    }

    @Override
    public Animation build(Context context) {
      AlphaAnimation animation = new AlphaAnimation(0f, 1f);
      animation.setDuration(durationMillis);
      return animation;
    }
  }

  /**
   * A Builder for {@link DrawableCrossFadeFactory}.
   */
  public static class Builder {
    private static final int DEFAULT_DURATION_MS = 300;
    private int durationMillis;
    private ViewAnimationFactory<Drawable> factory;
    private boolean isCrossFadeEnabled;

    public Builder() {
      this(DEFAULT_DURATION_MS);
    }

    /**
     * @param durationMillis The duration of both the default animation when no previous Drawable
     *     is present and the cross fade animation when a previous Drawable is present. This value
     *     will not be used by the default animation if {@link #setDefaultAnimationId(int)},
     *     {@link #setDefaultAnimation(Animation)}, or
     *     {@link #setDefaultAnimationFactory(ViewAnimationFactory)} is called.
     */
    public Builder(int durationMillis) {
      this.durationMillis = durationMillis;
      factory = new ViewAnimationFactory<>(
          new DefaultViewTransitionAnimationFactory(durationMillis));
    }

    /**
     * Enables or disables animating the alpha of the {@link Drawable} the cross fade will animate
     * from.
     *
     * <p>Defaults to {@code false}.
     *
     * @param isCrossFadeEnabled If {@code true} the previous {@link Drawable}'s alpha will be
     *     animated from 100 to 0 while the new {@link Drawable}'s alpha is
     *     animated from 0 to 100. Otherwise the previous {@link Drawable}'s
     *     alpha will remain at 100 throughout the animation. See
     *     {@link android.graphics.drawable.TransitionDrawable#setCrossFadeEnabled(boolean)}
     */
    public Builder setCrossFadeEnabled(boolean isCrossFadeEnabled) {
      this.isCrossFadeEnabled = isCrossFadeEnabled;
      return this;
    }

    /**
     * Sets the resource id of the {@link Animation} to use when no previous {@link Drawable} is
     * available to animate from.
     *
     * <p>Defaults to a simple fade in.
     */
    public Builder setDefaultAnimationId(int animationId) {
      return setDefaultAnimationFactory(new ViewAnimationFactory<Drawable>(animationId));
    }

    /**
     * Sets the {@link Animation} to use when no previous {@link Drawable} is available to animate
     * from.
     *
     * <p>It is not safe to use the same {@link Animation} object for multiple animations
     * simultaneously. Always pass in a new instance to this method.
     */
    public Builder setDefaultAnimation(Animation animation) {
      return setDefaultAnimationFactory(new ViewAnimationFactory<Drawable>(animation));
    }

    /**
     * Sets the {@link ViewAnimationFactory} to use to generate animations to animate when no
     * previous {@link Drawable} is available to animate from.
     */
    public Builder setDefaultAnimationFactory(ViewAnimationFactory<Drawable> factory) {
      this.factory = factory;
      return this;
    }

    public DrawableCrossFadeFactory build() {
      return new DrawableCrossFadeFactory(factory, durationMillis, isCrossFadeEnabled);
    }
  }
}
