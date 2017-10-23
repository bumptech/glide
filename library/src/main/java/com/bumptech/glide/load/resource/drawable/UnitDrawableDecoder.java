package com.bumptech.glide.load.resource.drawable;

import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import java.io.IOException;

/**
 * Passes through a {@link Drawable} as a {@link Drawable} based {@link Resource}.
 */
public class UnitDrawableDecoder implements ResourceDecoder<Drawable, Drawable> {
  @Override
  public boolean handles(Drawable source, Options options) throws IOException {
    return true;
  }

  @Nullable
  @Override
  public Resource<Drawable> decode(Drawable source, int width, int height, Options options)
      throws IOException {
    return NonOwnedDrawableResource.newInstance(source);
  }
}
