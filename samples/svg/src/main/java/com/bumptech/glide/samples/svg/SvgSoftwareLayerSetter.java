package com.bumptech.glide.samples.svg;

import android.graphics.drawable.PictureDrawable;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.ImageViewTarget;
import com.bumptech.glide.request.target.Target;

/**
 * Listener which updates the {@link ImageView} to be software rendered, because {@link
 * com.caverock.androidsvg.SVG SVG}/{@link android.graphics.Picture Picture} can't render on a
 * hardware backed {@link android.graphics.Canvas Canvas}.
 */
public class SvgSoftwareLayerSetter implements RequestListener<PictureDrawable> {

  @Override
  public boolean onLoadFailed(
      GlideException e, Object model, @NonNull Target<PictureDrawable> target, boolean isFirstResource) {
    ImageView view = ((ImageViewTarget<?>) target).getView();
    view.setLayerType(ImageView.LAYER_TYPE_NONE, null);
    return false;
  }

  @Override
  public boolean onResourceReady(
      @NonNull PictureDrawable resource,
      @NonNull Object model,
      Target<PictureDrawable> target,
      @NonNull DataSource dataSource,
      boolean isFirstResource) {
    ImageView view = ((ImageViewTarget<?>) target).getView();
    view.setLayerType(ImageView.LAYER_TYPE_SOFTWARE, null);
    return false;
  }
}
