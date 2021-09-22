package com.bumptech.glide.request.transition;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import com.bumptech.glide.load.DataSource;

/**
 * A {@link TransitionFactory} for complex types that have a {@link android.graphics.Bitmap} inside.
 * The transitioning bitmap is wrapped in a {@link android.graphics.drawable.BitmapDrawable}. Most
 * commonly used with {@link DrawableCrossFadeFactory}.
 *
 * @param <R> The type of the composite object that contains the {@link android.graphics.Bitmap} to
 *     be transitioned.
 */
public abstract class BitmapContainerTransitionFactory<R> implements TransitionFactory<R> {
  private final TransitionFactory<Drawable> realFactory;

  // Public API.
  @SuppressWarnings("WeakerAccess")
  public BitmapContainerTransitionFactory(TransitionFactory<Drawable> realFactory) {
    this.realFactory = realFactory;
  }

  @Override
  public Transition<R> build(DataSource dataSource, boolean isFirstResource) {
    Transition<Drawable> transition = realFactory.build(dataSource, isFirstResource);
    return new BitmapGlideAnimation(transition);
  }

  /**
   * Retrieve the Bitmap from a composite object.
   *
   * <p><b>Warning:</b> Do not convert any arbitrary object to Bitmap via expensive drawing here,
   * this method is called on the UI thread.
   *
   * @param current composite object containing a Bitmap and some other information
   * @return the Bitmap contained within {@code current}
   */
  protected abstract Bitmap getBitmap(R current);

  private final class BitmapGlideAnimation implements Transition<R> {
    private final Transition<Drawable> transition;

    BitmapGlideAnimation(Transition<Drawable> transition) {
      this.transition = transition;
    }

    @Override
    public boolean transition(R current, ViewAdapter adapter) {
      Resources resources = adapter.getView().getResources();
      Drawable currentBitmap = new BitmapDrawable(resources, getBitmap(current));
      return transition.transition(currentBitmap, adapter);
    }
  }
}
