package com.bumptech.glide;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import com.bumptech.glide.util.Util;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Tracks started {@link Activity} instances and Glide-managed lifecycles to trim memory caches and
 * switch {@link MemoryCategory} when all UI stops or when Glide initializes without visible UI,
 * including when Android suppresses {@link ComponentCallbacks2#TRIM_MEMORY_UI_HIDDEN} due to an
 * active foreground service keeping the process above {@code PROCESS_STATE_IMPORTANT_BACKGROUND}.
 */
final class AppVisibilityMemoryTrimmer implements Application.ActivityLifecycleCallbacks {
  private static final String TAG = "Glide";

  private final Glide glide;
  private final Set<Activity> startedActivities =
      Collections.newSetFromMap(new WeakHashMap<Activity, Boolean>());
  private volatile boolean isRegistered;

  private final Runnable initialBackgroundCheckRunnable =
      new Runnable() {
        @Override
        public void run() {
          if (isRegistered && !hasVisibleUi() && !isProcessInForegroundOrVisible()) {
            glide.setMemoryCategoryWhenInBackground();
          }
        }
      };

  private final Runnable trimIfBackgroundedRunnable =
      new Runnable() {
        @Override
        public void run() {
          if (!isRegistered || hasVisibleUi()) {
            return;
          }
          glide.trimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN);
          glide.setMemoryCategoryWhenInBackground();
        }
      };

  AppVisibilityMemoryTrimmer(@NonNull Glide glide) {
    this.glide = glide;
  }

  void register(@NonNull Context context) {
    Context applicationContext = context.getApplicationContext();
    if (!(applicationContext instanceof Application)) {
      if (Log.isLoggable(TAG, Log.WARN)) {
        Log.w(
            TAG,
            "Glide requires an Application Context. You passed: "
                + applicationContext
                + ". This will disable setting memory category in background.");
      }
      return;
    }
    isRegistered = true;
    ((Application) applicationContext).registerActivityLifecycleCallbacks(this);
    if (!isProcessInForegroundOrVisible()) {
      if (Util.isOnMainThread()) {
        if (!hasVisibleUi()) {
          glide.setMemoryCategoryWhenInBackground();
        }
      } else {
        Util.postOnUiThread(initialBackgroundCheckRunnable);
      }
    }
  }

  void unregister(@NonNull Context context) {
    isRegistered = false;
    Util.removeCallbacksOnUiThread(initialBackgroundCheckRunnable);
    Util.removeCallbacksOnUiThread(trimIfBackgroundedRunnable);
    Context applicationContext = context.getApplicationContext();
    if (applicationContext instanceof Application) {
      ((Application) applicationContext).unregisterActivityLifecycleCallbacks(this);
    }
  }

  @Override
  public void onActivityStarted(@NonNull Activity activity) {
    if (!isRegistered) {
      return;
    }
    startedActivities.add(activity);
    onUiVisible();
  }

  @Override
  public void onActivityResumed(@NonNull Activity activity) {
    if (!isRegistered) {
      return;
    }
    startedActivities.add(activity);
    onUiVisible();
  }

  @Override
  public void onActivityStopped(@NonNull Activity activity) {
    if (!isRegistered) {
      return;
    }
    startedActivities.remove(activity);
    // FragmentActivity.onStop() invokes super.onStop() (this callback) before calling
    // markFragmentsCreated(), so defer the full hasVisibleUi() check to the posted runnable.
    if (!activity.isChangingConfigurations() && startedActivities.isEmpty()) {
      Util.removeCallbacksOnUiThread(trimIfBackgroundedRunnable);
      Util.postOnUiThread(trimIfBackgroundedRunnable);
    }
  }

  @Override
  public void onActivityDestroyed(@NonNull Activity activity) {
    startedActivities.remove(activity);
  }

  @Override
  public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
    // Do nothing.
  }

  @Override
  public void onActivityPaused(@NonNull Activity activity) {
    // Do nothing.
  }

  @Override
  public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    // Do nothing.
  }

  private void onUiVisible() {
    Util.removeCallbacksOnUiThread(initialBackgroundCheckRunnable);
    Util.removeCallbacksOnUiThread(trimIfBackgroundedRunnable);
    glide.setMemoryCategoryWhenInForeground();
  }

  private boolean hasVisibleUi() {
    return !startedActivities.isEmpty() || glide.getRequestManagerRetriever().hasStartedLifecycle();
  }

  private static boolean isProcessInForegroundOrVisible() {
    ActivityManager.RunningAppProcessInfo processInfo = new ActivityManager.RunningAppProcessInfo();
    ActivityManager.getMyMemoryState(processInfo);
    return processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        || processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;
  }
}
