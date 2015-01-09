package com.bumptech.glide;

import com.bumptech.glide.request.animation.GlideAnimationFactory;
import com.bumptech.glide.request.animation.NoAnimation;
import com.bumptech.glide.request.animation.ViewAnimationFactory;
import com.bumptech.glide.request.animation.ViewPropertyAnimation;
import com.bumptech.glide.request.animation.ViewPropertyAnimationFactory;
import com.bumptech.glide.util.Preconditions;

/**
 * A base class for setting an animation to use to animate in a resource when the load completes.
 *
 * @param <CHILD> The implementation of this class to return to chain methods.
 * @param <TranscodeType> The type of resource that will be animated.
 */
public abstract class AnimationOptions<CHILD extends AnimationOptions<CHILD, TranscodeType>, TranscodeType>
        implements Cloneable {
    private GlideAnimationFactory<? super TranscodeType> animationFactory = NoAnimation.getFactory();

    /**
     * Removes any existing animation set on the builder. Will be overridden by subsequent calls that set an animation.
     * @return This request builder.
     */
    public final CHILD dontAnimate() {
        return animate(NoAnimation.getFactory());
    }

    /**
     * Sets an animation to run on the wrapped target when an resource load finishes. Will only be run if the resource
     * was loaded asynchronously (ie was not in the memory cache)
     *
     * @param animationId The resource id of the animation to run
     * @return This request builder.
     */
    public final CHILD animate(int animationId) {
        return animate(new ViewAnimationFactory<TranscodeType>(animationId));
    }

    /**
     * Sets an animator to run a {@link android.view.ViewPropertyAnimator} on a view that the target may be wrapping
     * when a resource load finishes. Will only be run if the load was loaded asynchronously (ie was not in the
     * memory cache).
     *
     * @param animator The {@link com.bumptech.glide.request.animation.ViewPropertyAnimation.Animator} to run.
     * @return This request builder.
     */
    public final CHILD animate(ViewPropertyAnimation.Animator animator) {
        return animate(new ViewPropertyAnimationFactory<TranscodeType>(animator));
    }

    public final CHILD animate(GlideAnimationFactory<? super TranscodeType> animationFactory) {
        this.animationFactory = Preconditions.checkNotNull(animationFactory);
        return self();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected final CHILD clone() {
        try {
            return (CHILD) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    final GlideAnimationFactory<? super TranscodeType> getAnimationFactory() {
        return animationFactory;
    }

    @SuppressWarnings("unchecked")
    private CHILD self() {
        return (CHILD) this;
    }
}
