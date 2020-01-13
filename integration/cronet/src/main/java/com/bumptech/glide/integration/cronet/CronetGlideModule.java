package com.bumptech.glide.integration.cronet;

import android.content.Context;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.GlideModule;
import com.google.android.apps.common.proguard.UsedByReflection;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * A {@link GlideModule} that registers components allowing remote image fetching to be done using
 * Cronet.
 */
@UsedByReflection("meta-data")
public final class CronetGlideModule implements GlideModule {

  @Override
  public void applyOptions(Context context, GlideBuilder builder) {}

  @Override
  public void registerComponents(Context context, Glide glide, Registry registry) {
    CronetRequestFactory factory =
        new CronetRequestFactoryImpl(() -> CronetEngineSingleton.getSingleton(context));
    registry.replace(
        GlideUrl.class, InputStream.class, new ChromiumUrlLoader.StreamFactory(factory, null));
    registry.prepend(
        GlideUrl.class, ByteBuffer.class, new ChromiumUrlLoader.ByteBufferFactory(factory, null));
  }
}
