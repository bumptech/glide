package com.bumptech.glide.manager;

import android.app.Activity;

final class DoNothingFirstFrameWaiter implements FirstFrameWaiter {

  @Override
  public void registerSelf(Activity activity) {
    // Do nothing.
  }
}
