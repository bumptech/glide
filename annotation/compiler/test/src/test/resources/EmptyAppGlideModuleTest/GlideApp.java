package com.bumptech.glide.test;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import com.bumptech.glide.Glide;
import java.io.File;
import java.lang.String;

/**
 * The entry point for interacting with Glide for Applications
 *
 * <p>Includes all generated APIs from all
 * {@link com.bumptech.glide.annotation.GlideExtension}s in source and dependent libraries.
 *
 * <p>This class is generated and should not be modified
 * @see Glide
 */
public final class GlideApp {
  private GlideApp() {
  }

  /**
   * @see Glide#getPhotoCacheDir(Context)
   */
  @Nullable
  public static File getPhotoCacheDir(Context context) {
    return Glide.getPhotoCacheDir(context);
  }

  /**
   * @see Glide#getPhotoCacheDir(Context, String)
   */
  @Nullable
  public static File getPhotoCacheDir(Context context, String cacheName) {
    return Glide.getPhotoCacheDir(context, cacheName);
  }

  /**
   * @see Glide#get(Context)
   */
  public static Glide get(Context context) {
    return Glide.get(context);
  }

  /**
   * @see Glide#init(Glide)
   */
  @VisibleForTesting
  @SuppressLint("VisibleForTests")
  public static void init(Glide glide) {
    Glide.init(glide);
  }

  /**
   * @see Glide#tearDown()
   */
  @VisibleForTesting
  @SuppressLint("VisibleForTests")
  public static void tearDown() {
    Glide.tearDown();
  }

  /**
   * @see Glide#with(Context)
   */
  public static GlideRequests with(Context context) {
    return (GlideRequests) Glide.with(context);
  }

  /**
   * @see Glide#with(Activity)
   */
  public static GlideRequests with(Activity activity) {
    return (GlideRequests) Glide.with(activity);
  }

  /**
   * @see Glide#with(FragmentActivity)
   */
  public static GlideRequests with(FragmentActivity activity) {
    return (GlideRequests) Glide.with(activity);
  }

  /**
   * @see Glide#with(Fragment)
   */
  public static GlideRequests with(Fragment fragment) {
    return (GlideRequests) Glide.with(fragment);
  }

  /**
   * @see Glide#with(Fragment)
   */
  public static GlideRequests with(android.support.v4.app.Fragment fragment) {
    return (GlideRequests) Glide.with(fragment);
  }

  /**
   * @see Glide#with(View)
   */
  public static GlideRequests with(View view) {
    return (GlideRequests) Glide.with(view);
  }
}
