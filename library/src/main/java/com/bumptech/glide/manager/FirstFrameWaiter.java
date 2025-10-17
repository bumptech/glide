package com.bumptech.glide.manager;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnDrawListener;
import androidx.annotation.RequiresApi;
import com.bumptech.glide.load.resource.bitmap.HardwareConfigState;
import com.bumptech.glide.util.Synthetic;
import com.bumptech.glide.util.Util;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

@RequiresApi(Build.VERSION_CODES.O)
final class FirstFrameWaiter implements FrameWaiter {
  @Synthetic
  final Set<Activity> pendingActivities =
      Collections.newSetFromMap(new WeakHashMap<Activity, Boolean>());

  @Synthetic volatile boolean isFirstFrameSet;

  @Override
  public void registerSelf(Activity activity) {
    // It's possible we'll create a few of these, but it's not particularly expensive to do so and
    // we'd rather work around any edge cases that might prevent the first Activity we listen to
    // from firing our callback ever.
    if (isFirstFrameSet) {
      return;
    }
    if (!pendingActivities.add(activity)) {
      return;
    }

    final View view = activity.getWindow().getDecorView();
    ViewTreeObserver viewTreeObserver = view.getViewTreeObserver();
    viewTreeObserver.addOnDrawListener(
        new OnDrawListener() {
          @Override
          public void onDraw() {
            // We can't remove the listener during onDraw, so always post the removal to the UI
            // thread, even if the first frame may already be set before our listener goes off.
            final OnDrawListener listener = this;
            Util.postOnUiThread(
                new Runnable() {
                  @Override
                  public void run() {
                    HardwareConfigState.getInstance().unblockHardwareBitmaps();
                    isFirstFrameSet = true;
                    removeListener(view, listener);
                    pendingActivities.clear();
                  }
                });
          }
        });
  }

  @Synthetic
  static void removeListener(View view, OnDrawListener listener) {
    // The original ViewTreeObserver might be merged into a new one and be dead.
    // Since we have to handle that case anyway, We might as well always just
    // obtain the current observer and use a single code path.
    // We also have to wait to remove this because we're being called in onDraw.
    ViewTreeObserver currentViewTreeObserver = view.getViewTreeObserver();
    currentViewTreeObserver.removeOnDrawListener(listener);
  }
}
