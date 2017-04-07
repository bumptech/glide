package com.bumptech.glide.samples.imgur;

import dagger.Subcomponent;
import dagger.android.AndroidInjector;

/**
 * The subcomponent for the Imgur sample's main activity.
 */
@Subcomponent
public interface MainActivitySubcomponent extends AndroidInjector<MainActivity> {

  /**
   * Dagger componeent for Activity injection.
   */
  @Subcomponent.Builder
  abstract class Builder extends AndroidInjector.Builder<MainActivity> {
    // Intentionally empty.
  }
}
