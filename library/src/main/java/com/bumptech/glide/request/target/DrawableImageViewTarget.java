package com.bumptech.glide.request.target;

import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.request.transition.Transition;

/**
 * A target for display {@link Drawable} objects in {@link ImageView}s.
 */
public class DrawableImageViewTarget extends ImageViewTarget<Drawable> {
  private Drawable resource;

  public DrawableImageViewTarget(ImageView view) {
    super(view);
  }

  @Override
  protected void setResource(Drawable resource) {
    this.resource = resource;
    view.setImageDrawable(resource);
  }

  @Override
  public void onResourceReady(Drawable resource, Transition<? super Drawable> transition) {
    ViewGroup.LayoutParams layoutParams = view.getLayoutParams();

    // This is a dirty hack that tries to make loading square thumbnails and then square full
    // images less costly by forcing both the smaller thumb and the larger version to have exactly
    // the same intrinsic dimensions. If a drawable is replaced in an ImageView by another drawable
    // with different intrinsic dimensions, the ImageView requests a layout. Scrolling rapidly while
    // replacing thumbs with larger images triggers lots of these calls and causes significant
    // amounts of jank.
    if (!(resource instanceof Animatable) && layoutParams != null && layoutParams.width > 0
        && layoutParams.height > 0) {
      resource = new FixedSizeDrawable(resource, layoutParams.width, layoutParams.height);
    }
    super.onResourceReady(resource, transition);
    if (resource instanceof Animatable) {
      ((Animatable) resource).start();
    }
  }

  @Override
  public void onStart() {
    if (resource instanceof Animatable) {
      ((Animatable) resource).start();
    }
  }

  @Override
  public void onStop() {
    if (resource instanceof Animatable) {
      ((Animatable) resource).stop();
    }
  }
}
