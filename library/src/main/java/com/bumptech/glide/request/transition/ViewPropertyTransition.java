package com.bumptech.glide.request.transition;

import android.view.View;

/**
 * A {@link Transition} that accepts an interface that can apply an animation like a {@link
 * android.view.ViewPropertyAnimator} or a {@link android.animation.ObjectAnimator} that can be used
 * to transition a resource into a {@link View}.
 *
 * @param <R> The type of the resource that will be transitioned into a view.
 */
public class ViewPropertyTransition<R> implements Transition<R> {

  private final Animator animator;
  private ViewAdapter viewAdapter;

  /**
   * Constructor for a view property animation that takes an {@link ViewPropertyTransition.Animator}
   * interface that can apply a transition to a view.
   *
   * @param animator The animator to use.
   */
  // Public API.
  @SuppressWarnings("WeakerAccess")
  public ViewPropertyTransition(Animator animator) {
    this.animator = animator;
  }

  /**
   * Always applies the {@link ViewPropertyTransition.Animator} given in the constructor to the
   * given view and returns {@code false} because the animator cannot put the new resource on the
   * view.
   *
   * @param current {@inheritDoc}
   * @param adapter {@inheritDoc}
   * @return {@inheritDoc}
   */
  @Override
  public boolean transition(R current, ViewAdapter adapter) {
    final View view = adapter.getView();
    if (view != null) {
      viewAdapter = adapter;
      animator.animate(adapter.getView());
    }
    return false;
  }

  @Override
  public void cancel() {
    if (viewAdapter != null) {
      final View view = viewAdapter.getView();
      if (view != null) {
        animator.cancel(view);
      }
      viewAdapter = null;
    }
  }

  /**
   * An interface that allows an animation to be applied on or started from an {@link
   * android.view.View}.
   */
  public interface Animator {
    /**
     * Starts an animation on the given {@link android.view.View}.
     *
     * @param view The view to transition.
     */
    void animate(View view);

    /**
     * Cancel an animation on the given {@link android.view.View}. This can be called even after
     * proper animation ending.
     *
     * @param view The view to cancel transition.
     */
    void cancel(View view);
  }
}
