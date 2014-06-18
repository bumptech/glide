package com.bumptech.glide.request;

import android.graphics.drawable.Drawable;
import android.view.View;
import com.bumptech.glide.request.target.Target;

public class NoAnimation implements GlideAnimation {
    private static final NoAnimation NO_ANIMATION = new NoAnimation();
    private static final GlideAnimationFactory NO_ANIMATION_FACTORY = new NoAnimationFactory();

    public static class NoAnimationFactory implements GlideAnimationFactory {
        @Override
        public GlideAnimation build(boolean isFromMemoryCache, boolean isFirstImage) {
            return NO_ANIMATION;
        }
    }

    @SuppressWarnings("unchecked")
    public static <R> GlideAnimationFactory<R> getFactory() {
        return NO_ANIMATION_FACTORY;
    }

    @SuppressWarnings("unchecked")
    public static <R> GlideAnimation<R> get() {
        return NO_ANIMATION;
    }

    @Override
    public boolean animate(Drawable previous, Object current, View view, Target target) {
        return false;
    }
}
