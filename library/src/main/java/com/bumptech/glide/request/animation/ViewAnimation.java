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

    private final AnimationFactory animationFactory;

    /**
     * Constructs a new ViewAnimation that will start the given {@link android.view.animation.Animation}.
     */
    ViewAnimation(AnimationFactory animationFactory) {
        this.animationFactory = animationFactory;
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
            Animation animation = animationFactory.build();
            view.startAnimation(animation);
        }

        return false;
    }

    /**
     * A {@link com.bumptech.glide.request.animation.GlideAnimationFactory} that produces ViewAnimations.
     */
    public static class ViewAnimationFactory<R> implements GlideAnimationFactory<R> {
        private final AnimationFactory animationFactory;
        private GlideAnimation<R> glideAnimation;

        public ViewAnimationFactory(Animation animation) {
            this(new ConcreteAnimationFactory(animation));
        }

        public ViewAnimationFactory(Context context, int animationId) {
            this(new ResourceAnimationFactory(context, animationId));
        }

        ViewAnimationFactory(AnimationFactory animationFactory) {
            this.animationFactory = animationFactory;
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
                glideAnimation = new ViewAnimation<R>(animationFactory);
            }

            return glideAnimation;
        }
    }

    private static class ConcreteAnimationFactory implements AnimationFactory {
        private final Animation animation;

        public ConcreteAnimationFactory(Animation animation) {
            this.animation = animation;
        }

        @Override
        public Animation build() {
            return animation;
        }
    }

    private static class ResourceAnimationFactory implements AnimationFactory {
        private final Context context;
        private final int animationId;

        public ResourceAnimationFactory(Context context, int animationId) {
            this.context = context.getApplicationContext();
            this.animationId = animationId;
        }

        @Override
        public Animation build() {
            return AnimationUtils.loadAnimation(context, animationId);
        }
    }

    interface AnimationFactory {
        Animation build();
    }
}
