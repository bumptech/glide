package com.bumptech.glide.request.animation;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

/**
 * A {@link com.bumptech.glide.request.animation.GlideAnimation GlideAnimation} that can apply a
 * {@link android.view.animation.Animation Animation} to a {@link android.view.View View} using
 * {@link android.view.View#startAnimation(android.view.animation.Animation) View.startAnimation}.
 *
 * @param <R> The type of the resource displayed in the view that is animated
 */
public class ViewAnimation<R> implements GlideAnimation<R> {

    /**
     * A {@link com.bumptech.glide.request.animation.GlideAnimationFactory} that produces ViewAnimations.
     */
    public static class ViewAnimationFactory<R> implements GlideAnimationFactory<R> {
        private Animation animation;
        private Context context;
        private int animationId;
        private GlideAnimation<R> glideAnimation;

        public ViewAnimationFactory(Animation animation) {
            this.animation = animation;
        }

        public ViewAnimationFactory(Context context, int animationId) {
            this.context = context;
            this.animationId = animationId;
        }

        /**
         * Returns a new {@link com.bumptech.glide.request.animation.GlideAnimation} for the given arguments. If
         * isFromMemoryCache is {@code true} or isFirstImage is {@code false}, returns a
         * {@link com.bumptech.glide.request.animation.NoAnimation} and otherwise returns a new
         * {@link com.bumptech.glide.request.animation.ViewAnimation}.
         *
         * @param isFromMemoryCache {@inheritDoc}
         * @param isFirstResource {@inheritDoc}
         */
        @Override
        public GlideAnimation<R> build(boolean isFromMemoryCache, boolean isFirstResource) {
            if (isFromMemoryCache || !isFirstResource) {
                return NoAnimation.get();
            }

            if (glideAnimation == null) {
                if (animation == null) {
                    animation = AnimationUtils.loadAnimation(context, animationId);
                }
                glideAnimation = new ViewAnimation<R>(animation);
            }

            return glideAnimation;
        }
    }

    private Animation animation;

    /**
     * Constructs a new ViewAnimation that will start the given {@link android.view.animation.Animation}.
     * @param animation The animation to use.
     */
    public ViewAnimation(Animation animation) {
        this.animation = animation;
    }

    /**
     * Always clears the current animation on the view using {@link android.view.View#clearAnimation()}, then
     * starts the {@link android.view.animation.Animation} given in the constructor using
     * {@link android.view.View#startAnimation(android.view.animation.Animation)} and then returns {@code false} because
     * the animation does not actually set the current resource on the view.
     *
     * @param current {@inheritDoc}
     * @param adapter {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public boolean animate(R current, ViewAdapter adapter) {
        View view = adapter.getView();
        if (view != null) {
            view.clearAnimation();

            view.startAnimation(animation);
        }

        return false;
    }
}
