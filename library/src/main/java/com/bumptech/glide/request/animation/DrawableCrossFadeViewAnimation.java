package com.bumptech.glide.request.animation;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

/**
 * A cross fade {@link GlideAnimation} for {@link android.graphics.drawable.Drawable}s
 * that uses an {@link android.graphics.drawable.TransitionDrawable} to transition from an existing drawable
 * already visible on the target to a new drawable. If no existing drawable exists, this class can instead fall back
 * to a default animation that doesn't rely on {@link android.graphics.drawable.TransitionDrawable}.
 */
public class DrawableCrossFadeViewAnimation<T extends Drawable> implements GlideAnimation<T> {
    // 150 ms.
    public static final int DEFAULT_DURATION = 300;
    private Animation defaultAnimation;
    private int duration;

    private static Animation getDefaultAnimation() {
        AlphaAnimation animation = new AlphaAnimation(0f, 1f);
        animation.setDuration(DEFAULT_DURATION / 2);
        return animation;
    }

    /**
     * A factory class that produces a new {@link GlideAnimation} that varies depending
     * on whether or not the drawable was loaded from the memory cache and whether or not the drawable is the first
     * image to be set on the target.
     *
     * <p>
     *     Resources are usually loaded from the memory cache just before the user can see the view,
     *     for example when the user changes screens or scrolls back and forth in a list. In those cases the user
     *     typically does not expect to see an animation. As a result, when the resource is loaded from the memory
     *     cache this factory producdes an {@link NoAnimation}.
     * </p>
     */
    public static class DrawableCrossFadeFactory<T extends Drawable> implements GlideAnimationFactory<T> {
        private Context context;
        private int defaultAnimationId;
        private Animation defaultAnimation;
        private int duration;
        private DrawableCrossFadeViewAnimation<T> animation;

        public DrawableCrossFadeFactory() {
            this(getDefaultAnimation(), DEFAULT_DURATION);
        }

        public DrawableCrossFadeFactory(int duration) {
            this(getDefaultAnimation(), duration);
        }

        public DrawableCrossFadeFactory(Context context, int defaultAnimationId, int duration) {
            this.context = context;
            this.defaultAnimationId = defaultAnimationId;
            this.duration = duration;
        }

        public DrawableCrossFadeFactory(Animation defaultAnimation, int duration) {
            this.defaultAnimation = defaultAnimation;
            this.duration = duration;
        }

        @Override
        public GlideAnimation<T> build(boolean isFromMemoryCache, boolean isFirstResource) {
            if (isFromMemoryCache) {
                return NoAnimation.get();
            }

            if (animation == null) {
                if (defaultAnimation == null) {
                    defaultAnimation = AnimationUtils.loadAnimation(context, defaultAnimationId);
                }
                animation = new DrawableCrossFadeViewAnimation<T>(defaultAnimation, duration);
            }

            return animation;
        }
    }

    /**
     * Constructor that takes a default animation and a duration in milliseconds that the cross fade animation should
     * last.
     * @param defaultAnimation The default animation that will run if there is nothing to cross fade from when a new
     *                         {@link android.graphics.drawable.Drawable} is set.
     * @param duration The duration that the cross fade animation should run if there is something to cross fade from
     *                 when a new {@link android.graphics.drawable.Drawable} is set.
     */
    public DrawableCrossFadeViewAnimation(Animation defaultAnimation, int duration) {
        this.defaultAnimation = defaultAnimation;
        this.duration = duration;
    }

    /**
     * Animates from the previous drawable to the current drawable in one of two ways.
     *
     * <ol>
     *     <li>Using the default animation provided in the constructor if the previous drawable is null</li>
     *     <li>Using the cross fade animation with the duration provided in the constructor if the previous
     *     drawable is non null</li>
     * </ol>
     *
     * @param previous The previous drawable that is currently being displayed in the {@link android.view.View}.
     * @param current The new drawable that should be displayed in the {@link com.bumptech.glide.request.target.Target}
     *                after this animation completes.
     * @param view The {@link android.widget.ImageView} the animation should run on.
     * @return true if in the process of running the animation the current drawable is set on the view, false if the
     * current drawable must be set on the view manually by the caller of this method.
     */
    @Override
    public boolean animate(Drawable previous, T current, ImageView view) {
        if (previous != null) {
            TransitionDrawable transitionDrawable = new TransitionDrawable(new Drawable[] { previous, current });
            transitionDrawable.setCrossFadeEnabled(true);
            transitionDrawable.startTransition(duration);
            view.setImageDrawable(transitionDrawable);
            return true;
        } else {
            view.startAnimation(defaultAnimation);
            return false;
        }
    }
}
