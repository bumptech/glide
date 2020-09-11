package com.bumptech.glide.manager;

import android.app.Activity;
import android.os.Build;
import androidx.annotation.RequiresApi;

@RequiresApi(Build.VERSION_CODES.O)
final class FirstFrameWaiterO implements FirstFrameWaiter {

  @Override
  public void registerSelf(Activity activity) {}
}
