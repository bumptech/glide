package com.bumptech.glide.samples.imgur;

import com.bumptech.glide.samples.imgur.api.ApiModule;
import dagger.Component;
import dagger.android.AndroidInjectionModule;
import dagger.android.AndroidInjector;
import javax.inject.Singleton;

/** Specifies Dagger modules for {@link ImgurApplication}. */
@Singleton
@Component(
    modules = {
      AndroidInjectionModule.class,
      MainActivityModule.class,
      ApplicationModule.class,
      ApiModule.class
    })
public interface ImgurApplicationComponent extends AndroidInjector<ImgurApplication> {}
