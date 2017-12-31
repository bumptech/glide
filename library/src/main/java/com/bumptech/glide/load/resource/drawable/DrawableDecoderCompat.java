package com.bumptech.glide.load.resource.drawable;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.content.res.AppCompatResources;

/**
 * Handles decoding Drawables with the v7 support library if present and falling back to the v4
 * support library otherwise.
 */
public final class DrawableDecoderCompat {
  private static volatile boolean shouldCallAppCompatResources = true;

  private DrawableDecoderCompat() {
    // Utility class.
  }

  /**
   * See {@code getDrawable(Context, int, Theme)}.
   */
  public static Drawable getDrawable(Context context, @DrawableRes int drawableId) {
    return getDrawable(context, drawableId, /*theme=*/ null);
  }

  /**
   * Loads a Drawable using {@link AppCompatResources} if available and {@link ResourcesCompat}
   * otherwise, depending on whether or not the v7 support library is included in the application.
   *
   * @param theme Used instead of the {@link Theme} returned from the given {@link Context} if
   *              non-null when loading the {@link Drawable}.
   */
  public static Drawable getDrawable(
      Context context, @DrawableRes int drawableId, @Nullable Theme theme) {
    try {
      // Race conditions may cause us to attempt to load using v7 more than once. That's ok since
      // this check is a modest optimization and the output will be correct anyway.
      if (shouldCallAppCompatResources) {
        return loadDrawableV7(context, drawableId);
      }
    } catch (NoClassDefFoundError error) {
      shouldCallAppCompatResources = false;
    } catch (Resources.NotFoundException e) {
      // Ignored, this can be thrown when drawable compat attempts to decode a canary resource. If
      // that decode attempt fails, we still want to try with the v4 ResourcesCompat below.
    }

    return loadDrawableV4(context, drawableId, theme != null ? theme : context.getTheme());
  }

  private static Drawable loadDrawableV7(Context context, @DrawableRes int drawableId) {
    return AppCompatResources.getDrawable(context, drawableId);
  }

  private static Drawable loadDrawableV4(
      Context context, @DrawableRes int drawableId, @Nullable Theme theme) {
    Resources resources = context.getResources();
    return ResourcesCompat.getDrawable(resources, drawableId, theme);
  }
}
