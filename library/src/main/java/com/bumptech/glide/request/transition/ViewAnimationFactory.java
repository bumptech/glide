package com.bumptech.glide.request.transition;

import android.content.Context;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.bumptech.glide.load.DataSource;

/**
 * A {@link TransitionFactory} that produces {@link ViewTransition}s.
 *
 * @param <R> The type of the resource that will be transitioned into a view.
 */
public class ViewAnimationFactory<R> implements TransitionFactory<R> {
  private final ViewTransition.ViewTransitionAnimationFactory viewTransitionAnimationFactory;
  private Transition<R> transition;

  // Public API.
  @SuppressWarnings("unused")
  public ViewAnimationFactory(Animation animation) {
    this(new ConcreteViewTransitionAnimationFactory(animation));
  }

  public ViewAnimationFactory(int animationId) {
    this(new ResourceViewTransitionAnimationFactory(animationId));
  }

  ViewAnimationFactory(
      ViewTransition.ViewTransitionAnimationFactory viewTransitionAnimationFactory) {
    this.viewTransitionAnimationFactory = viewTransitionAnimationFactory;
  }

  /**
   * Returns a new {@link Transition} for the given arguments. If isFromMemoryCache is {@code true}
   * or isFirstImage is {@code false}, returns a {@link NoTransition} and otherwise returns a new
   * {@link ViewTransition}.
   *
   * @param dataSource {@inheritDoc}
   * @param isFirstResource {@inheritDoc}
   */
  @Override
  public Transition<R> build(DataSource dataSource, boolean isFirstResource) {
    if (dataSource == DataSource.MEMORY_CACHE || !isFirstResource) {
      return NoTransition.get();
    }

    if (transition == null) {
      transition = new ViewTransition<>(viewTransitionAnimationFactory);
    }

    return transition;
  }

  private static class ConcreteViewTransitionAnimationFactory
      implements ViewTransition.ViewTransitionAnimationFactory {
    private final Animation animation;

    ConcreteViewTransitionAnimationFactory(Animation animation) {
      this.animation = animation;
    }

    @Override
    public Animation build(Context context) {
      return animation;
    }
  }

  private static class ResourceViewTransitionAnimationFactory
      implements ViewTransition.ViewTransitionAnimationFactory {
    private final int animationId;

    ResourceViewTransitionAnimationFactory(int animationId) {
      this.animationId = animationId;
    }

    @Override
    public Animation build(Context context) {
      return AnimationUtils.loadAnimation(context, animationId);
    }
  }
}
