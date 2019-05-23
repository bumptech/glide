package com.bumptech.glide.integration.okhttp;

import android.content.Context;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.module.LibraryGlideModule;
import java.io.InputStream;

/**
 * Registers OkHttp related classes via Glide's annotation processor.
 *
 * <p>For Applications that depend on this library and include an {@link AppGlideModule} and Glide's
 * annotation processor, this class will be automatically included.
 *
 * @deprecated Prefer the okhttp3 version instead.
 */
@GlideModule
@Deprecated
public class OkHttpLibraryGlideModule extends LibraryGlideModule {
  @Override
  public void registerComponents(Context context, Glide glide, Registry registry) {
    registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory());
  }
}
