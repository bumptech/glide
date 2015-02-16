package com.bumptech.glide.request.transition;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

/**
 * A factory class that produces a new {@link Transition} that varies depending on whether or not
 * the drawable was loaded from the memory cache and whether or not the drawable is the first image
 * to be put on the target.
 *
 * <p> Resources are usually loaded from the memory cache just before the user can see the view, for
 * example when the user changes screens or scrolls back and forth in a list. In those cases the
 * user typically does not expect to see a transition. As a result, when the resource is loaded from
 * the memory cache this factory produces an {@link NoTransition}. </p>
 */
public class DrawableCrossFadeFactory implements TransitionFactory<Drawable> {
  private static final int DEFAULT_DURATION_MS = 300;
  private final ViewAnimationFactory<Drawable> viewAnimationFactory;
  private final int duration;
  private DrawableCrossFadeTransition transition;

  public DrawableCrossFadeFactory() {
    this(DEFAULT_DURATION_MS);
  }

  public DrawableCrossFadeFactory(int duration) {
    this(new ViewAnimationFactory<Drawable>(new DefaultViewTransitionAnimationFactory()), duration);
  }

  public DrawableCrossFadeFactory(int defaultAnimationId, int duration) {
    this(new ViewAnimationFactory<Drawable>(defaultAnimationId), duration);
  }

  public DrawableCrossFadeFactory(Animation defaultAnimation, int duration) {
    this(new ViewAnimationFactory<Drawable>(defaultAnimation), duration);
  }

  DrawableCrossFadeFactory(ViewAnimationFactory<Drawable> viewAnimationFactory, int duration) {
    this.viewAnimationFactory = viewAnimationFactory;
    this.duration = duration;
  }

  @Override
  public Transition<Drawable> build(boolean isFromMemoryCache, boolean isFirstResource) {
    if (isFromMemoryCache) {
      return NoTransition.get();
    }

    if (transition == null) {
      Transition<Drawable> defaultTransition = viewAnimationFactory.build(false, isFirstResource);
      transition = new DrawableCrossFadeTransition(defaultTransition, duration);
    }

    return transition;
  }

  private static class DefaultViewTransitionAnimationFactory implements ViewTransition
      .ViewTransitionAnimationFactory {

    @Override
    public Animation build(Context context) {
      AlphaAnimation animation = new AlphaAnimation(0f, 1f);
      animation.setDuration(DEFAULT_DURATION_MS / 2);
      return animation;
    }
  }
}
