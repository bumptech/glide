package com.bumptech.glide.request.animation;

/**
 * A {@link GlideAnimationFactory} that produces ViewPropertyAnimations.
 *
 * @param <R> The type of the resource displayed in the view that is animated
 */
public class ViewPropertyAnimationFactory<R> implements GlideAnimationFactory<R> {
    private final ViewPropertyAnimation.Animator animator;
    private ViewPropertyAnimation<R> animation;

    public ViewPropertyAnimationFactory(ViewPropertyAnimation.Animator animator) {
        this.animator = animator;
    }

    /**
     * Returns a new {@link GlideAnimation} for the given arguments. If
     * isMemoryCache is {@code true} or isFirstImage is {@code false}, returns a
     * {@link NoAnimation} and otherwise returns a new
     * {@link ViewPropertyAnimation} for the
     * {@link com.bumptech.glide.request.animation.ViewPropertyAnimation.Animator} provided in the constructor.
     */
    @Override
    public GlideAnimation<R> build(boolean isFromMemoryCache, boolean isFirstResource) {
        if (isFromMemoryCache || !isFirstResource) {
            return NoAnimation.get();
        }
        if (animation == null) {
            animation = new ViewPropertyAnimation<R>(animator);
        }

        return animation;
    }
}
