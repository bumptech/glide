package com.bumptech.glide.load.resource.drawable;

import android.graphics.drawable.Drawable;
import com.bumptech.glide.TransitionOptions;
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory;
import com.bumptech.glide.request.transition.TransitionFactory;

/**
 * Contains {@link Drawable} specific animation options.
 */
public final class DrawableTransitionOptions extends
    TransitionOptions<DrawableTransitionOptions, Drawable> {

  /**
   * Returns a {@link DrawableTransitionOptions} object that enables a cross fade animation.
   *
   * @see #crossFade()
   */
  public static DrawableTransitionOptions withCrossFade() {
    return new DrawableTransitionOptions().crossFade();
  }

  /**
   * Returns a {@link DrawableTransitionOptions} object that enables a cross fade animation.
   *
   * @see #crossFade(int)
   */
  public static DrawableTransitionOptions withCrossFade(int duration) {
    return new DrawableTransitionOptions().crossFade(duration);
  }

  /**
   * Returns a {@link DrawableTransitionOptions} object that enables a cross fade animation.
   *
   * @see #crossFade(int, int)
   */
  public static DrawableTransitionOptions withCrossFade(int animationId, int duration) {
    return new DrawableTransitionOptions().crossFade(animationId, duration);
  }

  /**
   * Returns a {@link DrawableTransitionOptions} object that enables a cross fade animation.
   *
   * @see #crossFade(DrawableCrossFadeFactory)
   */
  public static DrawableTransitionOptions withCrossFade(
      DrawableCrossFadeFactory drawableCrossFadeFactory) {
    return new DrawableTransitionOptions().crossFade(drawableCrossFadeFactory);
  }

  /**
   * Returns a {@link DrawableTransitionOptions} object that enables a cross fade animation.
   *
   * @see #crossFade(DrawableCrossFadeFactory.Builder)
   */
  public static DrawableTransitionOptions withCrossFade(
      DrawableCrossFadeFactory.Builder builder) {
    return new DrawableTransitionOptions().crossFade(builder);
  }

  /**
   * Returns a {@link DrawableTransitionOptions} object that uses the given transition factory.
   *
   * @see com.bumptech.glide.GenericTransitionOptions#with(TransitionFactory)
   */
  public static DrawableTransitionOptions with(
      TransitionFactory<Drawable> transitionFactory) {
    return new DrawableTransitionOptions().transition(transitionFactory);
  }

  /**
   * Enables a cross fade animation between both the placeholder and the first resource and between
   * subsequent resources (if thumbnails are used).
   */
  public DrawableTransitionOptions crossFade() {
    return crossFade(new DrawableCrossFadeFactory.Builder());
  }

  /**
   * Enables a cross fade animation between both the placeholder and the first resource and between
   * subsequent resources (if thumbnails are used).
   *
   * @param duration The duration of the animation, see
   *     {@code DrawableCrossFadeFactory.Builder(int)}
   * @see com.bumptech.glide.request.transition.DrawableCrossFadeFactory.Builder
   */
  public DrawableTransitionOptions crossFade(int duration) {
    return crossFade(new DrawableCrossFadeFactory.Builder(duration));
  }

  /**
   * Enables a cross fade animation between both the placeholder and the first resource and between
   * subsequent resources (if thumbnails are used).
   *
   * @param animationId The id of the animation to use if no placeholder or previous resource is
   *     set, see {@code DrawableCrossFadeFactory.Builder#setDefaultAnimationId(int)}.
   * @param duration The duration of the cross fade, see
   *     {@code DrawableCrossFadeFactory.Builder(int)}
   * @see com.bumptech.glide.request.transition.DrawableCrossFadeFactory.Builder
   */
  public DrawableTransitionOptions crossFade(int animationId, int duration) {
    return crossFade(
        new DrawableCrossFadeFactory.Builder(duration)
            .setDefaultAnimationId(animationId));
  }

  /**
   * Enables a cross fade animation between both the placeholder and the first resource and between
   * subsequent resources (if thumbnails are used).
   */
  public DrawableTransitionOptions crossFade(DrawableCrossFadeFactory drawableCrossFadeFactory) {
    return transition(drawableCrossFadeFactory);
  }

  /**
   * Enables a cross fade animation between both the placeholder and the first resource and between
   * subsequent resources (if thumbnails are used).
   */
  public DrawableTransitionOptions crossFade(DrawableCrossFadeFactory.Builder builder) {
    return crossFade(builder.build());
  }
}

