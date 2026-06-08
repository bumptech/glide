package com.bumptech.glide.testutil;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.os.Looper;
import android.view.Display;
import android.view.View;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowActivityManager;
import org.robolectric.shadows.ShadowBitmap;
import org.robolectric.shadows.ShadowConnectivityManager;
import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.shadows.ShadowDisplay;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowPackageManager;
import org.robolectric.shadows.ShadowView;

/**
 * Custom shadows helper to avoid using {@link org.robolectric.Shadows} which references removed
 * APIs (like FingerprintManager) and causes compilation failures.
 */
public final class CustomShadows {

  private CustomShadows() {}

  public static ShadowLooper shadowOf(Looper looper) {
    return (ShadowLooper) Shadow.extract(looper);
  }

  public static ShadowDisplay shadowOf(Display display) {
    return (ShadowDisplay) Shadow.extract(display);
  }

  public static ShadowView shadowOf(View view) {
    return (ShadowView) Shadow.extract(view);
  }

  public static ShadowActivityManager shadowOf(ActivityManager activityManager) {
    return (ShadowActivityManager) Shadow.extract(activityManager);
  }

  public static ShadowContentResolver shadowOf(ContentResolver contentResolver) {
    return (ShadowContentResolver) Shadow.extract(contentResolver);
  }

  public static ShadowBitmap shadowOf(Bitmap bitmap) {
    return (ShadowBitmap) Shadow.extract(bitmap);
  }

  public static ShadowPackageManager shadowOf(PackageManager packageManager) {
    return (ShadowPackageManager) Shadow.extract(packageManager);
  }

  public static ShadowConnectivityManager shadowOf(ConnectivityManager connectivityManager) {
    return (ShadowConnectivityManager) Shadow.extract(connectivityManager);
  }
}
