package com.bumptech.glide.load.model;

import androidx.annotation.Nullable;

/** An optional interface that models can implement to enhance control over Glide behaviors. */
public interface Model {

  /**
   * Returns {@code true} if this model produces the same image using the same mechanism (server,
   * authentication, source etc) as the given model.
   *
   * <p>Models must also override {@link Object#equals(Object other)} and {@link Object#hashCode()}
   * to ensure that caching functions correctly. If this object returns {@code true} from this
   * method for a given Model, it must also be equal to and have the same hash code as the given
   * model.
   *
   * <p>However, this model may be equal to and have the same hash code as a given model but still
   * return {@code false} from this method. This method optionally allows you to differentiate
   * between Models that load the same image via multiple different means. For example one Model
   * might load the image from server A and another model might load the same image from server B.
   * The models must be equal to each other with the same hash code because they load the same
   * image. However two requests made with the different models are not exactly the same because the
   * way the image is loaded will differ.
   */
  boolean isEquivalentTo(@Nullable Object other);
}
