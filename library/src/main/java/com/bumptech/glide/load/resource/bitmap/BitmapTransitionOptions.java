package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import com.bumptech.glide.TransitionOptions;
import com.bumptech.glide.request.transition.BitmapTransitionFactory;
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory;
import com.bumptech.glide.request.transition.TransitionFactory;

/**
 * Contains {@link Bitmap} specific animation options.
 */
public final class BitmapTransitionOptions extends
    TransitionOptions<BitmapTransitionOptions, Bitmap> {

  /**
   * Returns a {@link BitmapTransitionOptions} object that enables a cross fade animation.
   *
   * @see #crossFade()
   */
  public static BitmapTransitionOptions withCrossFade() {
    return new BitmapTransitionOptions().crossFade();
  }

  /**
   * Returns a {@link BitmapTransitionOptions} object that enables a cross fade animation.
   *
   * @see #crossFade(int)
   */
  public static BitmapTransitionOptions withCrossFade(int duration) {
    return new BitmapTransitionOptions().crossFade(duration);
  }

  /**
   * Returns a {@link BitmapTransitionOptions} object that enables a cross fade animation.
   *
   * @see #crossFade(int, int)
   */
  public static BitmapTransitionOptions withCrossFade(int animationId, int duration) {
    return new BitmapTransitionOptions().crossFade(animationId, duration);
  }

  /**
   * Returns a {@link BitmapTransitionOptions} object that enables a cross fade animation.
   *
   * @see #crossFade(DrawableCrossFadeFactory)
   */
  public static BitmapTransitionOptions withCrossFade(
      DrawableCrossFadeFactory drawableCrossFadeFactory) {
    return new BitmapTransitionOptions().crossFade(drawableCrossFadeFactory);
  }

  /**
   * Returns a {@link BitmapTransitionOptions} object that enables a cross fade animation.
   *
   * @see #crossFade(DrawableCrossFadeFactory.Builder)
   */
  public static BitmapTransitionOptions withCrossFade(
      DrawableCrossFadeFactory.Builder builder) {
    return new BitmapTransitionOptions().crossFade(builder);
  }

  /**
   * Returns a {@link BitmapTransitionOptions} object that enables a any animation
   * that is possible on drawables.
   *
   * @see #transitionUsing(TransitionFactory)
   */
  public static BitmapTransitionOptions withWrapped(
      TransitionFactory<Drawable> drawableCrossFadeFactory) {
    return new BitmapTransitionOptions().transitionUsing(drawableCrossFadeFactory);
  }

  /**
   * Returns a {@link BitmapTransitionOptions} object that uses the given transition factory.
   *
   * @see com.bumptech.glide.GenericTransitionOptions#with(TransitionFactory)
   */
  public static BitmapTransitionOptions with(
      TransitionFactory<Bitmap> transitionFactory) {
    return new BitmapTransitionOptions().transition(transitionFactory);
  }

  /**
   * Enables a cross fade animation between both the placeholder and the first resource and between
   * subsequent resources (if thumbnails are used).
   */
  public BitmapTransitionOptions crossFade() {
    return crossFade(new DrawableCrossFadeFactory.Builder());
  }

  /**
   * Enables a cross fade animation between both the placeholder and the first resource and between
   * subsequent resources (if thumbnails are used).
   *
   * @param duration The duration of the animation, see
   *     {@code DrawableCrossFadeFactory.Builder(int)}.
   * @see com.bumptech.glide.request.transition.DrawableCrossFadeFactory.Builder
   */
  public BitmapTransitionOptions crossFade(int duration) {
    return crossFade(new DrawableCrossFadeFactory.Builder(duration));
  }

  /**
   * Enables a cross fade animation between both the placeholder and the first resource and between
   * subsequent resources (if thumbnails are used).
   *
   * @param animationId The id of the animation to use if no placeholder or previous resource is
   *     set, see {@code DrawableCrossFadeFactory.Builder#setDefaultAnimationId(int)}.
   * @param duration The duration of the cross fade, see
   *     {@code DrawableCrossFadeFactory.Builder(int)}.
   * @see com.bumptech.glide.request.transition.DrawableCrossFadeFactory.Builder
   */
  public BitmapTransitionOptions crossFade(int animationId, int duration) {
    return crossFade(
        new DrawableCrossFadeFactory.Builder(duration)
            .setDefaultAnimationId(animationId));
  }

  /**
   * Enables a cross fade animation between both the placeholder and the first resource and between
   * subsequent resources (if thumbnails are used).
   */
  public BitmapTransitionOptions crossFade(DrawableCrossFadeFactory drawableCrossFadeFactory) {
    return transitionUsing(drawableCrossFadeFactory);
  }

  /**
   * Enables a any Drawable based animation to run on Bitmaps as well.
   */
  public BitmapTransitionOptions transitionUsing(
      TransitionFactory<Drawable> drawableCrossFadeFactory) {
    return transition(new BitmapTransitionFactory(drawableCrossFadeFactory));
  }

  /**
   * Enables a cross fade animation between both the placeholder and the first resource and between
   * subsequent resources (if thumbnails are used).
   */
  public BitmapTransitionOptions crossFade(DrawableCrossFadeFactory.Builder builder) {
    return transitionUsing(builder.build());
  }
}

