package com.bumptech.glide.samples.imgur;

import dagger.android.AndroidInjector;
import dagger.android.support.DaggerApplication;

/** Runs Dagger injection in the Imgur sample. */
public final class ImgurApplication extends DaggerApplication {
  @Override
  protected AndroidInjector<? extends DaggerApplication> applicationInjector() {
    return DaggerImgurApplicationComponent.create();
  }
}
