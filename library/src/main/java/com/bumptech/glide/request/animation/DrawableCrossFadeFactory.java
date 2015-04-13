package com.bumptech.glide.request.animation;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

/**
 * A factory class that produces a new {@link com.bumptech.glide.request.animation.GlideAnimation} that varies depending
 * on whether or not the drawable was loaded from the memory cache and whether or not the drawable is the first
 * image to be set on the target.
 *
 * <p>
 *     Resources are usually loaded from the memory cache just before the user can see the view,
 *     for example when the user changes screens or scrolls back and forth in a list. In those cases the user
 *     typically does not expect to see an animation. As a result, when the resource is loaded from the memory
 *     cache this factory produces an {@link com.bumptech.glide.request.animation.NoAnimation}.
 * </p>
 *
 * @param <T> The type of the {@link android.graphics.drawable.Drawable} that will be animated.
 */
public class DrawableCrossFadeFactory<T extends Drawable> implements GlideAnimationFactory<T> {
    private static final int DEFAULT_DURATION_MS = 300;
    private final ViewAnimationFactory<T> animationFactory;
    private final int duration;
    private DrawableCrossFadeViewAnimation<T> firstResourceAnimation;
    private DrawableCrossFadeViewAnimation<T> secondResourceAnimation;

    public DrawableCrossFadeFactory() {
        this(DEFAULT_DURATION_MS);
    }

    public DrawableCrossFadeFactory(int duration) {
        this(new ViewAnimationFactory<T>(new DefaultAnimationFactory(duration)), duration);
    }

    public DrawableCrossFadeFactory(Context context, int defaultAnimationId, int duration) {
        this(new ViewAnimationFactory<T>(context, defaultAnimationId), duration);
    }

    public DrawableCrossFadeFactory(Animation defaultAnimation, int duration) {
        this(new ViewAnimationFactory<T>(defaultAnimation), duration);
    }

    DrawableCrossFadeFactory(ViewAnimationFactory<T> animationFactory, int duration) {
        this.animationFactory = animationFactory;
        this.duration = duration;
    }

    @Override
    public GlideAnimation<T> build(boolean isFromMemoryCache, boolean isFirstResource) {
        if (isFromMemoryCache) {
            return NoAnimation.get();
        } else if (isFirstResource) {
            return getFirstResourceAnimation();
        } else {
            return getSecondResourceAnimation();
        }
    }

    private GlideAnimation<T> getFirstResourceAnimation() {
        if (firstResourceAnimation == null) {
            GlideAnimation<T> defaultAnimation = animationFactory.build(false /*isFromMemoryCache*/,
                true /*isFirstResource*/);
            firstResourceAnimation = new DrawableCrossFadeViewAnimation<T>(defaultAnimation, duration);
        }
        return firstResourceAnimation;
    }

    private GlideAnimation<T> getSecondResourceAnimation() {
        if (secondResourceAnimation == null) {
            GlideAnimation<T> defaultAnimation = animationFactory.build(false /*isFromMemoryCache*/,
                false /*isFirstResource*/);
            secondResourceAnimation = new DrawableCrossFadeViewAnimation<T>(defaultAnimation, duration);
        }
        return secondResourceAnimation;
    }

    private static class DefaultAnimationFactory implements ViewAnimation.AnimationFactory {

        private final int duration;

        DefaultAnimationFactory(int duration) {
            this.duration = duration;
        }

        @Override
        public Animation build() {
            AlphaAnimation animation = new AlphaAnimation(0f, 1f);
            animation.setDuration(duration);
            return animation;
        }
    }
}
