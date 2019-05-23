package com.bumptech.glide.samples.imgur;

import com.bumptech.glide.samples.imgur.api.ApiModule;
import dagger.Component;
import dagger.android.AndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;
import javax.inject.Singleton;

/** Specifies Dagger modules for {@link ImgurApplication}. */
@Singleton
@Component(
    modules = {
      AndroidSupportInjectionModule.class,
      MainActivityModule.class,
      ApplicationModule.class,
      ApiModule.class
    })
public interface ImgurApplicationComponent extends AndroidInjector<ImgurApplication> {
  // Empty.
}
