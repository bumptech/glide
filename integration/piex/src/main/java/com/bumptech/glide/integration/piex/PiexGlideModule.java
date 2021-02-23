package com.bumptech.glide.integration.piex;

import android.content.Context;
import android.graphics.Bitmap;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.module.GlideModule;
import com.google.android.apps.common.proguard.UsedByReflection;
import java.io.InputStream;

/** GlideModule for the Piex library. */
@UsedByReflection("meta-data")
public class PiexGlideModule implements GlideModule {

  @Override
  public void applyOptions(Context context, GlideBuilder builder) {
    // Do nothing.
  }

  @Override
  public void registerComponents(Context context, Glide glide, Registry registry) {
    registry.prepend(InputStream.class, Bitmap.class, new PiexResourceDecoder(glide));
  }
}
