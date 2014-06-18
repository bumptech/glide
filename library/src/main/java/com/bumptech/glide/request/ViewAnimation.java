package com.bumptech.glide.request;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.bumptech.glide.request.target.Target;

public class ViewAnimation implements GlideAnimation {

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

    public ViewAnimation(Animation animation) {
        this.animation = animation;
    }

    @Override
    public boolean animate(Drawable previous, Object current, View view, Target target) {
        view.clearAnimation();

        view.startAnimation(animation);

        return false;
    }
}
