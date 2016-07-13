package com.bumptech.glide;

import com.bumptech.glide.request.transition.TransitionFactory;
import com.bumptech.glide.request.transition.ViewPropertyTransition;

/**
 * Implementation of {@link TransitionOptions} that exposes only generic methods that can be applied
 * to any resource type.
 *
 * @param <TranscodeType> The type of the resource that will be displayed.
 */
@SuppressWarnings("PMD.UseUtilityClass")
public final class GenericTransitionOptions<TranscodeType> extends
    TransitionOptions<GenericTransitionOptions<TranscodeType>, TranscodeType> {
  /**
   * Removes any existing animation put on the builder.
   *
   * @see GenericTransitionOptions#dontTransition()
   */
  public static <TranscodeType> GenericTransitionOptions<TranscodeType> withNoTransition() {
    return new GenericTransitionOptions<TranscodeType>().dontTransition();
  }

  /**
   * Returns a typed {@link GenericTransitionOptions} object that uses the given view animation.
   *
   * @see GenericTransitionOptions#transition(int)
   */
  public static <TranscodeType> GenericTransitionOptions<TranscodeType> with(
      int viewAnimationId) {
    return new GenericTransitionOptions<TranscodeType>().transition(viewAnimationId);
  }

  /**
   * Returns a typed {@link GenericTransitionOptions} object that uses the given animator.
   *
   * @see GenericTransitionOptions#transition(ViewPropertyTransition.Animator)
   */
  public static <TranscodeType> GenericTransitionOptions<TranscodeType> with(
      ViewPropertyTransition.Animator animator) {
    return new GenericTransitionOptions<TranscodeType>().transition(animator);
  }

  /**
   * Returns a typed {@link GenericTransitionOptions} object that uses the given transition factory.
   *
   * @see GenericTransitionOptions#transition(TransitionFactory)
   */
  public static <TranscodeType> GenericTransitionOptions<TranscodeType> with(
      TransitionFactory<? super TranscodeType> transitionFactory) {
    return new GenericTransitionOptions<TranscodeType>().transition(transitionFactory);
  }
}
