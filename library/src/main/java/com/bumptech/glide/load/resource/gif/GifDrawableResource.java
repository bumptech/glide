package com.bumptech.glide.load.resource.gif;

import android.graphics.Bitmap;

import com.bumptech.glide.load.resource.drawable.DrawableResource;
import com.bumptech.glide.util.Util;

/**
 * A resource wrapping an {@link com.bumptech.glide.load.resource.gif.GifDrawable}.
 */
public class GifDrawableResource extends DrawableResource<GifDrawable> {
  public GifDrawableResource(GifDrawable drawable) {
    super(drawable);
  }

  @Override
  public Class<GifDrawable> getResourceClass() {
    return GifDrawable.class;
  }

  @Override
  public int getSize() {
    Bitmap firstFrame = drawable.getFirstFrame();
   return drawable.getBuffer().limit() + Util.getBitmapByteSize(firstFrame.getWidth(),
       firstFrame.getHeight(), firstFrame.getConfig());
  }

  @Override
  public void recycle() {
    drawable.stop();
    drawable.recycle();
  }
}
