package com.bumptech.glide.manager;

import android.app.Activity;

interface FrameWaiter {
  void registerSelf(Activity activity);
}
