package com.bumptech.glide.load.resource.gif;

import com.bumptech.glide.AnimationOptions;
import com.bumptech.glide.request.animation.DrawableCrossFadeFactory;

public final class GifAnimationOptions extends AnimationOptions<GifAnimationOptions, GifDrawable> {

    public GifAnimationOptions crossFade() {
        return animate(new DrawableCrossFadeFactory());
    }

    public GifAnimationOptions crossFade(int duration) {
        return animate(new DrawableCrossFadeFactory(duration));
    }

    public GifAnimationOptions crossFade(int animationId, int duration) {
        return animate(new DrawableCrossFadeFactory(animationId, duration));
    }
}
