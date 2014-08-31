package com.bumptech.glide.request.animation;

import android.view.View;

/**
 * A {@link com.bumptech.glide.request.animation.GlideAnimation GlideAnimation} that accepts an interface
 * that can apply an animation like a {@link android.view.ViewPropertyAnimator}
 * or a {@link android.animation.ObjectAnimator} to an {@link View}.
 *
 * @param <R> The type of the resource displayed in the view that is animated
 */
public class ViewPropertyAnimation<R> implements GlideAnimation<R> {

    /**
     * An interface that allows an animation to be applied on or started from an {@link android.view.View}.
     */
    public interface Animator {
        /**
         * Starts an animation on the given {@link android.view.View}.
         *
         * @param view The view to animate.
         */
        public void animate(View view);
    }

    /**
     * A {@link com.bumptech.glide.request.animation.GlideAnimationFactory} that produces ViewPropertyAnimations.
     */
    public static class ViewPropertyAnimationFactory<R> implements GlideAnimationFactory<R> {
        private Animator animator;
        private ViewPropertyAnimation<R> animation;

        public ViewPropertyAnimationFactory(Animator animator) {
            this.animator = animator;
        }

        /**
         * Returns a new {@link com.bumptech.glide.request.animation.GlideAnimation} for the given arguments. If
         * isMemoryCache is {@code true} or isFirstImage is {@code false}, returns a
         * {@link com.bumptech.glide.request.animation.NoAnimation} and otherwise returns a new
         * {@link com.bumptech.glide.request.animation.ViewPropertyAnimation} for the
         * {@link com.bumptech.glide.request.animation.ViewPropertyAnimation.Animator} provided in the constructor.
         */
        @Override
        public GlideAnimation<R> build(boolean isFromMemoryCache, boolean isFirstResource) {
            if (isFromMemoryCache || !isFirstResource) {
                return NoAnimation.get();
            }
            if (animation == null) {
                animation = new ViewPropertyAnimation<R>(animator);
            }

            return animation;
        }
    }

    private Animator animator;

    /**
     * Constructor for a view property animation that takes an
     * {@link com.bumptech.glide.request.animation.ViewPropertyAnimation.Animator} interface that can apply an animation
     * to a view.
     *
     * @param animator The animator to use.
     */
    public ViewPropertyAnimation(Animator animator) {
        this.animator = animator;
    }

    /**
     * Always applies the {@link com.bumptech.glide.request.animation.ViewPropertyAnimation.Animator} given in the
     * constructor to the given view and returns {@code false} because the animator cannot set the new resource on
     * the view.
     *
     * @param current {@inheritDoc}
     * @param adapter {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public boolean animate(R current, ViewAdapter adapter) {
        final View view = adapter.getView();
        if (view != null) {
            animator.animate(adapter.getView());
        }
        return false;
    }
}
