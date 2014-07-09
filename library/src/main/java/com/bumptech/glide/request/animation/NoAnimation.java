package com.bumptech.glide.request.animation;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

/**
 * A simple {@link com.bumptech.glide.request.animation.GlideAnimation} that performs no actions.
 */
public class NoAnimation implements GlideAnimation {
    private static final NoAnimation NO_ANIMATION = new NoAnimation();
    private static final GlideAnimationFactory NO_ANIMATION_FACTORY = new NoAnimationFactory();

    public static class NoAnimationFactory implements GlideAnimationFactory {
        @Override
        public GlideAnimation build(boolean isFromMemoryCache, boolean isFirstImage) {
            return NO_ANIMATION;
        }
    }

    /**
     * Returns an instance of a factory that produces {@link com.bumptech.glide.request.animation.NoAnimation}s.
     */
    @SuppressWarnings("unchecked")
    public static <R> GlideAnimationFactory<R> getFactory() {
        return NO_ANIMATION_FACTORY;
    }

    /**
     * Returns an instance of {@link com.bumptech.glide.request.animation.NoAnimation}.
     */
    @SuppressWarnings("unchecked")
    public static <R> GlideAnimation<R> get() {
        return NO_ANIMATION;
    }

    /**
     * Performs no animation and always returns {@code false}.
     */
    @Override
    public boolean animate(Drawable previous, Object current, ImageView view) {
        return false;
    }
}
