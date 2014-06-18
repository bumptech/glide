package com.bumptech.glide.request;

import android.graphics.drawable.Drawable;
import android.view.View;
import com.bumptech.glide.request.target.Target;

public class ViewPropertyAnimation implements GlideAnimation {

    public interface Animator {
        public void animate(View view);
    }

    public static class ViewPropertyAnimationFactory implements GlideAnimationFactory {
        private Animator animator;
        private ViewPropertyAnimation animation;

        public ViewPropertyAnimationFactory(Animator animator) {
            this.animator = animator;
        }

        @Override
        public GlideAnimation build(boolean isFromMemoryCache, boolean isFirstImage) {
            if (isFromMemoryCache || !isFirstImage) {
                return NoAnimation.get();
            }
            if (animation == null) {
                animation = new ViewPropertyAnimation(animator);
            }

            return animation;
        }
    }

    private Animator animator;

    public ViewPropertyAnimation(Animator animator) {
        this.animator = animator;
    }

    @Override
    public boolean animate(Drawable previous, Object current, View view, Target target) {
        animator.animate(view);
        return false;
    }
}
