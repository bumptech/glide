package com.bumptech.glide;

import android.view.ViewTreeObserver.OnPreDrawListener;
import java.util.function.Function;

/**
 * A class for holding static methods that are used to globally modify the behavior of Glide. This
 * class is used to allow apps to globally modify the behavior of Glide.
 */
public final class GlidePlugins {

  private static Function<OnPreDrawListener, OnPreDrawListener> onPreDrawListenerDecorator = null;

  /**
   * Sets a decorator to be applied to all {@link OnPreDrawListener}s created by Glide.
   *
   * <p>This is intended to be used by apps that want to globally modify all {@link
   * OnPreDrawListener}s created by Glide.
   *
   * <p>This is an experimental method that may be removed without warning in a future version.
   */
  public static void experimentalSetOnPreDrawListenerDecorator(
      Function<OnPreDrawListener, OnPreDrawListener> decorator) {
    onPreDrawListenerDecorator = decorator;
  }

  /**
   * Returns the {@link OnPreDrawListener} provided, possibly after being decorated by {@link
   * #experimentalSetOnPreDrawListenerDecorator(Function)}.
   */
  public static OnPreDrawListener decorateOnPreDrawListener(OnPreDrawListener listener) {
    if (onPreDrawListenerDecorator == null) {
      return listener;
    }
    return onPreDrawListenerDecorator.apply(listener);
  }

  private GlidePlugins() {}
}
