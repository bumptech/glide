package com.bumptech.glide.samples.imgur;

import android.app.Activity;
import android.app.Application;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import javax.inject.Inject;

/**
 * Runs Dagger injection in the Imgur sample.
 */
public final class ImgurApplication extends Application
    implements HasActivityInjector {
  @Inject DispatchingAndroidInjector<Activity> dispatchingActivityInjector;

  @Override
  public void onCreate() {
    super.onCreate();
    DaggerImgurApplicationComponent.create()
        .inject(this);
  }

  @Override
  public DispatchingAndroidInjector<Activity> activityInjector() {
    return dispatchingActivityInjector;
  }
}
