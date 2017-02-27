package com.bumptech.glide.integration.gifdecoder;

import com.bumptech.glide.load.engine.Initializable;
import com.bumptech.glide.load.resource.drawable.DrawableResource;

/**
 * A resource wrapping an {@link GifDrawable}.
 */
public class GifDrawableResource extends DrawableResource<GifDrawable>
    implements Initializable {
  public GifDrawableResource(GifDrawable drawable) {
    super(drawable);
  }

  @Override
  public Class<GifDrawable> getResourceClass() {
    return GifDrawable.class;
  }

  @Override
  public int getSize() {
   return drawable.getSize();
  }

  @Override
  public void recycle() {
    drawable.stop();
    drawable.recycle();
  }

  @Override
  public void initialize() {
    drawable.getFirstFrame().prepareToDraw();
  }
}
