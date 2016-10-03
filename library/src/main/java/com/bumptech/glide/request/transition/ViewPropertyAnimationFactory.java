package com.bumptech.glide.request.transition;

import com.bumptech.glide.load.DataSource;

/**
 * A {@link TransitionFactory} that produces ViewPropertyAnimations.
 *
 * @param <R> The type of the resource that will be transitioned into a view.
 */
public class ViewPropertyAnimationFactory<R> implements TransitionFactory<R> {
  private final ViewPropertyTransition.Animator animator;
  private ViewPropertyTransition<R> animation;

  public ViewPropertyAnimationFactory(ViewPropertyTransition.Animator animator) {
    this.animator = animator;
  }

  /**
   * Returns a new {@link Transition} for the given arguments. If isMemoryCache is {@code true} or
   * isFirstImage is {@code false}, returns a {@link NoTransition} and otherwise returns a new
   * {@link ViewPropertyTransition} for the {@link ViewPropertyTransition.Animator} provided in the
   * constructor.
   */
  @Override
  public Transition<R> build(DataSource dataSource, boolean isFirstResource) {
    if (dataSource == DataSource.MEMORY_CACHE || !isFirstResource) {
      return NoTransition.get();
    }
    if (animation == null) {
      animation = new ViewPropertyTransition<>(animator);
    }

    return animation;
  }
}
