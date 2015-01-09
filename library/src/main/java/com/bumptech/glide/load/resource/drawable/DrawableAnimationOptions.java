package com.bumptech.glide.load.resource.drawable;

import android.graphics.drawable.Drawable;

import com.bumptech.glide.AnimationOptions;
import com.bumptech.glide.request.animation.DrawableCrossFadeFactory;

/**
 * Contains {@link Drawable} specific animation options.
 */
public final class DrawableAnimationOptions extends AnimationOptions<DrawableAnimationOptions, Drawable> {

    public static DrawableAnimationOptions withCrossFade() {
        return new DrawableAnimationOptions().crossFade();
    }

    public static DrawableAnimationOptions withCrossFade(int duration) {
        return new DrawableAnimationOptions().crossFade(duration);
    }

    public static DrawableAnimationOptions withCrossFade(int animationId, int duration) {
        return new DrawableAnimationOptions().crossFade(animationId, duration);
    }

    public DrawableAnimationOptions crossFade() {
        return animate(new DrawableCrossFadeFactory());
    }

    public DrawableAnimationOptions crossFade(int duration) {
        return animate(new DrawableCrossFadeFactory(duration));
    }

    public DrawableAnimationOptions crossFade(int animationId, int duration) {
        return animate(new DrawableCrossFadeFactory(animationId, duration));
    }
}

