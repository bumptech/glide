package com.bumptech.glide.manager;

import android.app.Activity;
import android.content.ComponentCallbacks2;
import android.content.res.Configuration;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

@RequiresApi(Build.VERSION_CODES.O)
final class FirstFrameAndAfterTrimMemoryWaiter implements FrameWaiter, ComponentCallbacks2 {

  @Override
  public void registerSelf(Activity activity) {}

  @Override
  public void onTrimMemory(int level) {}

  @Override
  public void onConfigurationChanged(@NonNull Configuration newConfig) {}

  @Override
  public void onLowMemory() {
    onTrimMemory(TRIM_MEMORY_UI_HIDDEN);
  }
}
