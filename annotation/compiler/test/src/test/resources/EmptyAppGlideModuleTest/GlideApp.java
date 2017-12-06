package com.bumptech.glide.test;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import java.io.File;
import java.lang.Deprecated;
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
  public static File getPhotoCacheDir(Context arg0) {
    return Glide.getPhotoCacheDir(arg0);
  }

  /**
   * @see Glide#getPhotoCacheDir(Context, String)
   */
  @Nullable
  public static File getPhotoCacheDir(Context arg0, String arg1) {
    return Glide.getPhotoCacheDir(arg0, arg1);
  }

  /**
   * @see Glide#get(Context)
   */
  @NonNull
  public static Glide get(Context arg0) {
    return Glide.get(arg0);
  }

  /**
   * @see Glide#init(Glide)
   */
  @Deprecated
  @VisibleForTesting
  @SuppressLint("VisibleForTests")
  public static void init(Glide glide) {
    Glide.init(glide);
  }

  /**
   * @see Glide#init(Context, GlideBuilder)
   */
  @VisibleForTesting
  @SuppressLint("VisibleForTests")
  public static void init(Context arg0, GlideBuilder arg1) {
    Glide.init(arg0, arg1);
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
  public static GlideRequests with(Context arg0) {
    return (GlideRequests) Glide.with(arg0);
  }

  /**
   * @see Glide#with(Activity)
   */
  public static GlideRequests with(Activity arg0) {
    return (GlideRequests) Glide.with(arg0);
  }

  /**
   * @see Glide#with(FragmentActivity)
   */
  public static GlideRequests with(FragmentActivity arg0) {
    return (GlideRequests) Glide.with(arg0);
  }

  /**
   * @see Glide#with(Fragment)
   */
  public static GlideRequests with(Fragment arg0) {
    return (GlideRequests) Glide.with(arg0);
  }

  /**
   * @see Glide#with(Fragment)
   */
  public static GlideRequests with(android.support.v4.app.Fragment arg0) {
    return (GlideRequests) Glide.with(arg0);
  }

  /**
   * @see Glide#with(View)
   */
  public static GlideRequests with(View arg0) {
    return (GlideRequests) Glide.with(arg0);
  }
}
