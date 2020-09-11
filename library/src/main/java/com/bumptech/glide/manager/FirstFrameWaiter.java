package com.bumptech.glide.manager;

import android.app.Activity;

interface FirstFrameWaiter {
  void registerSelf(Activity activity);
}
