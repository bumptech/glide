package com.bumptech.glide.test;

import static org.mockito.Mockito.any;

import android.graphics.drawable.Drawable;
import com.bumptech.glide.request.target.Target;

/**
 * Mockito matchers for various common classes.
 */
public final class Matchers {

  private Matchers() {
    // Utility class.
  }

  @SuppressWarnings("unchecked")
  public static Target<Drawable> anyTarget() {
    return (Target<Drawable>) any(Target.class);
  }

  public static Drawable anyDrawable() {
    return any(Drawable.class);
  }
}
