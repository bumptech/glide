package com.bumptech.glide.request.animation;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.bumptech.glide.request.target.Target;

/**
 * An {@link com.bumptech.glide.request.animation.GlideAnimation} that can apply a
 * {@link android.view.animation.Animation} to a {@link android.view.View} using
 * {@link android.view.View#startAnimation(android.view.animation.Animation)}.
 */
public class ViewAnimation implements GlideAnimation {

    /**
     * A {@link com.bumptech.glide.request.animation.GlideAnimationFactory} that produces ViewAnimations.
     */
    public static class ViewAnimationFactory implements GlideAnimationFactory {
        private Animation animation;
        private Context context;
        private int animationId;
        private GlideAnimation glideAnimation;

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
         * @param isFirstImage {@inheritDoc}
         */
        @Override
        public GlideAnimation build(boolean isFromMemoryCache, boolean isFirstImage) {
            if (isFromMemoryCache || !isFirstImage) {
                return NoAnimation.get();
            }

            if (glideAnimation == null) {
                if (animation == null) {
                    animation = AnimationUtils.loadAnimation(context, animationId);
                }
                glideAnimation = new ViewAnimation(animation);
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
     * {@link View#startAnimation(android.view.animation.Animation)} and then returns {@code false} because the
     * animation does not actually set the current resource on the view.
     *
     * @param previous {@inheritDoc}
     * @param current {@inheritDoc}
     * @param view {@inheritDoc}
     * @param target {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public boolean animate(Drawable previous, Object current, View view, Target target) {
        view.clearAnimation();

        view.startAnimation(animation);

        return false;
    }
}
