package com.bumptech.glide.integration.cronet;

import android.content.Context;
import androidx.annotation.NonNull;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.cronet.ChromiumUrlLoader.ByteBufferFactory;
import com.bumptech.glide.integration.cronet.ChromiumUrlLoader.StreamFactory;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.LibraryGlideModule;
import com.google.common.base.Supplier;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.chromium.net.CronetEngine;

/**
 * A {@link LibraryGlideModule} that registers components allowing remote image fetching to be done
 * using Cronet.
 */
@GlideModule
public final class CronetLibraryGlideModule extends LibraryGlideModule {

  @Override
  public void registerComponents(
      final @NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
    CronetRequestFactory factory =
        new CronetRequestFactoryImpl(
            new Supplier<CronetEngine>() {
              @Override
              public CronetEngine get() {
                return CronetEngineSingleton.getSingleton(context);
              }
            });
    registry.replace(
        GlideUrl.class, InputStream.class, new StreamFactory(factory, null /* dataLogger */));
    registry.prepend(
        GlideUrl.class, ByteBuffer.class, new ByteBufferFactory(factory, null /* dataLogger */));
  }
}
