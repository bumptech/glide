package com.bumptech.glide.load.engine;

/**
 * A callback allowing a resource to do some optimization on a background thread before being
 * returned to the ui.
 */
public interface Initializable {

  /** Called on a background thread so the {@link Resource} can do some eager initialization. */
  void initialize();
}
