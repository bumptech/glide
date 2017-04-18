package com.bumptech.glide.samples.giphy;

import android.content.Context;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;
import java.io.InputStream;

/**
 * Configures Glide for the Giphy sample app.
 */
@GlideModule
public class GiphyGlideModule extends AppGlideModule {
  @Override
  public void registerComponents(Context context, Registry registry) {
    registry.append(Api.GifResult.class, InputStream.class, new GiphyModelLoader.Factory());
  }

  // Disable manifest parsing to avoid adding similar modules twice.
  @Override
  public boolean isManifestParsingEnabled() {
    return false;
  }
}
