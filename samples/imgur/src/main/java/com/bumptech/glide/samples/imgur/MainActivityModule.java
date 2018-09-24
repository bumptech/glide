package com.bumptech.glide.samples.imgur;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
abstract class MainActivityModule {
  @ContributesAndroidInjector
  abstract MainActivity contributeMainActivityInjector();
}
