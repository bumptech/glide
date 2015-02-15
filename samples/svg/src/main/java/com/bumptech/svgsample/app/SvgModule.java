package com.bumptech.svgsample.app;

import android.content.Context;
import android.graphics.drawable.PictureDrawable;

import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.module.GlideModule;
import com.caverock.androidsvg.SVG;

import java.io.InputStream;

/**
 * Module for the SVG sample app.
 */
public class SvgModule implements GlideModule {
  @Override
  public void applyOptions(Context context, GlideBuilder builder) {
    // Do nothing.
  }

  @Override
  public void registerComponents(Context context, Registry registry) {
    registry.register(SVG.class, PictureDrawable.class, new SvgDrawableTranscoder())
        .append(InputStream.class, SVG.class, new SvgDecoder());
  }
}
