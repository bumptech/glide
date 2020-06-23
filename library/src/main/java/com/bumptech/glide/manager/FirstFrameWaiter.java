package com.bumptech.glide.manager;

import android.app.Activity;
import android.view.View;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

final class FirstFrameWaiter {
  private final Set<View> pendingDecorViews =
      Collections.newSetFromMap(new WeakHashMap<View, Boolean>());

  void registerSelf(Activity activity) {}
}
