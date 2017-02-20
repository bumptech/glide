package com.bumptech.glide.integration.okhttp;

import android.content.Context;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.ChildGlideModule;
import java.io.InputStream;

/**
 * Registers OkHttp related classes via Glide's annotation processor.
 *
 * <p>For Applications that depend on this library and include an
 * {@link com.bumptech.glide.module.RootGlideModule} and Glide's annotation processor, this class
 * will be automatically included.
 */
@GlideModule
public class OkHttpChildGlideModule extends ChildGlideModule {
  @Override
  public void registerComponents(Context context, Registry registry) {
    registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory());
  }
}
