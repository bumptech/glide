package com.bumptech.glide;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
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
  private final Runnable initialBackgroundCheckRunnable = this::runInitialBackgroundCheck;
  private final Runnable trimIfBackgroundedRunnable = this::trimIfBackgrounded;
  private boolean isTrimRunnableScheduled;
  private volatile boolean pendingInitialBackgroundCheck;
  private volatile boolean isTrimPending;

  AppVisibilityMemoryTrimmer(Glide glide) {
    this.glide = glide;
  }

  void register(Context context) {
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
    boolean isBackgroundAtInit = !isProcessInForegroundOrVisible();
    boolean onMainThread = Util.isOnMainThread();
    if (isBackgroundAtInit && !onMainThread) {
      pendingInitialBackgroundCheck = true;
    }
    ((Application) applicationContext).registerActivityLifecycleCallbacks(this);
    if (isBackgroundAtInit) {
      if (onMainThread) {
        if (!hasVisibleUi()) {
          glide.setMemoryCategoryWhenInBackground();
        }
      } else {
        Util.postOnUiThread(initialBackgroundCheckRunnable);
      }
    }
  }

  void unregister(Context context) {
    Context applicationContext = context.getApplicationContext();
    if (applicationContext instanceof Application) {
      ((Application) applicationContext).unregisterActivityLifecycleCallbacks(this);
    }
    pendingInitialBackgroundCheck = false;
    isTrimPending = false;
  }

  @Override
  public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    // Do nothing.
  }

  @Override
  public void onActivityStarted(Activity activity) {
    startedActivities.add(activity);
    onUiVisible();
  }

  @Override
  public void onActivityResumed(Activity activity) {
    // Do nothing.
  }

  @Override
  public void onActivityPaused(Activity activity) {
    // Do nothing.
  }

  @Override
  public void onActivityStopped(Activity activity) {
    startedActivities.remove(activity);
    // FragmentActivity.onStop() invokes super.onStop() (this callback) before calling
    // markFragmentsCreated(), so defer the full hasVisibleUi() check to the posted runnable.
    if (!activity.isChangingConfigurations() && startedActivities.isEmpty()) {
      isTrimPending = true;
      if (!isTrimRunnableScheduled) {
        isTrimRunnableScheduled = true;
        Util.postOnUiThread(trimIfBackgroundedRunnable);
      }
    }
  }

  @Override
  public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    // Do nothing.
  }

  @Override
  public void onActivityDestroyed(Activity activity) {
    // Do nothing.
  }

  private void onUiVisible() {
    pendingInitialBackgroundCheck = false;
    isTrimPending = false;
    glide.setMemoryCategoryWhenInForeground();
  }

  private void runInitialBackgroundCheck() {
    if (pendingInitialBackgroundCheck && !hasVisibleUi()) {
      pendingInitialBackgroundCheck = false;
      glide.setMemoryCategoryWhenInBackground();
    }
  }

  private void trimIfBackgrounded() {
    isTrimRunnableScheduled = false;
    if (!isTrimPending || hasVisibleUi()) {
      isTrimPending = false;
      return;
    }
    isTrimPending = false;
    glide.trimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN);
    glide.setMemoryCategoryWhenInBackground();
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
