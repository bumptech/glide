package com.bumptech.glide.samples.flickr;

import android.content.Context;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.module.GlideModule;
import com.bumptech.glide.samples.flickr.api.Photo;
import java.io.InputStream;

/**
 * {@link com.bumptech.glide.module.GlideModule} for the Flickr sample app.
 */
public class FlickrGlideModule implements GlideModule {
  @Override
  public void applyOptions(Context context, GlideBuilder builder) {
    // Do nothing.
  }

  @Override
  public void registerComponents(Context context, Registry registry) {
    registry.append(Photo.class, InputStream.class, new FlickrModelLoader.Factory());
  }
}
