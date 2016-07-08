package com.bumptech.glide.samples.giphy;

import android.content.Context;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.module.GlideModule;
import java.io.InputStream;

/**
 * {@link com.bumptech.glide.module.GlideModule} implementation for the Giphy sample app.
 */
public class GiphyGlideModule implements GlideModule {
  @Override
  public void applyOptions(Context context, GlideBuilder builder) {
    // Do nothing.
  }

  @Override
  public void registerComponents(Context context, Registry registry) {
    registry.append(Api.GifResult.class, InputStream.class, new GiphyModelLoader.Factory());
  }
}
