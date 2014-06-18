package com.bumptech.glide.request;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.bumptech.glide.request.target.Target;

public class DrawableCrossFadeViewAnimation implements GlideAnimation<Drawable> {
    // 150 ms.
    public static final int DEFAULT_DURATION = 300;
    private Animation defaultAnimation;
    private int duration;

    private static Animation getDefaultAnimation() {
        AlphaAnimation animation = new AlphaAnimation(0f, 1f);
        animation.setDuration(DEFAULT_DURATION / 2);
        return animation;
    }

    public static class DrawableCrossFadeFactory implements GlideAnimationFactory<Drawable> {
        private Context context;
        private int defaultAnimationId;
        private Animation defaultAnimation;
        private int duration;
        private DrawableCrossFadeViewAnimation animation;

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
        public GlideAnimation<Drawable> build(boolean isFromMemoryCache, boolean isFirstImage) {
            if (isFromMemoryCache) {
                return NoAnimation.get();
            }

            if (animation == null) {
                if (defaultAnimation == null) {
                    defaultAnimation = AnimationUtils.loadAnimation(context, defaultAnimationId);
                }
                animation = new DrawableCrossFadeViewAnimation(defaultAnimation, duration);
            }

            return animation;
        }
    }

    public DrawableCrossFadeViewAnimation(Animation defaultAnimation, int duration) {
        this.defaultAnimation = defaultAnimation;
        this.duration = duration;
    }

    @Override
    public boolean animate(Drawable previous, Drawable current, View view, Target<Drawable> target) {
        if (previous != null) {
            TransitionDrawable transitionDrawable = new TransitionDrawable(new Drawable[] { previous, current });
            transitionDrawable.setCrossFadeEnabled(true);
            transitionDrawable.startTransition(duration);
            GlideAnimation<Drawable> none = NoAnimation.get();
            target.onResourceReady(transitionDrawable, none);
            return true;
        } else {
            view.startAnimation(defaultAnimation);
            return false;
        }
    }
}
