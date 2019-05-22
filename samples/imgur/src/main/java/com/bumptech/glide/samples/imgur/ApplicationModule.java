package com.bumptech.glide.samples.imgur;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;

/** The Application Dagger module for the Imgur sample. */
@Module
class ApplicationModule {
  @Provides
  OkHttpClient okHttpClient() {
    return new OkHttpClient();
  }
}
