package com.bumptech.glide.request.animation;

import android.graphics.drawable.Drawable;
import android.view.View;
import com.bumptech.glide.request.target.Target;

/**
 * An {@link com.bumptech.glide.request.animation.GlideAnimation} that accepts an interface that can apply an
 * animation like an {@link android.view.ViewPropertyAnimator} or a {@link android.animation.ObjectAnimator} to
 * an {@link View}.
 */
public class ViewPropertyAnimation implements GlideAnimation {

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
    public static class ViewPropertyAnimationFactory implements GlideAnimationFactory {
        private Animator animator;
        private ViewPropertyAnimation animation;

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
        public GlideAnimation build(boolean isFromMemoryCache, boolean isFirstImage) {
            if (isFromMemoryCache || !isFirstImage) {
                return NoAnimation.get();
            }
            if (animation == null) {
                animation = new ViewPropertyAnimation(animator);
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
     * @param previous {@inheritDoc}
     * @param current {@inheritDoc}
     * @param view {@inheritDoc}
     * @param target {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public boolean animate(Drawable previous, Object current, View view, Target target) {
        animator.animate(view);
        return false;
    }
}
