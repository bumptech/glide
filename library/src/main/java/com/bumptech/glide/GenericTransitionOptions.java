package com.bumptech.glide;

/**
 * Implementation of {@link TransitionOptions} that exposes only generic methods that can be applied
 * to any resource type.
 *
 * @param <TranscodeType> The type of the resource that will be displayed.
 */
@SuppressWarnings("PMD.UseUtilityClass")
public final class GenericTransitionOptions<TranscodeType> extends
  TransitionOptions<GenericTransitionOptions<TranscodeType>, TranscodeType> {

  public static <TranscodeType> GenericTransitionOptions<TranscodeType> withNoTransition() {
    return new GenericTransitionOptions<TranscodeType>().dontTransition();
  }
}
