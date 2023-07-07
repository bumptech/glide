package com.bumptech.glide.samples.flickr;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import androidx.appcompat.widget.AppCompatImageView;

/** An always square {@link ImageView}. */
public final class SquareImageView extends AppCompatImageView {

  public SquareImageView(Context context) {
    super(context);
  }

  public SquareImageView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public SquareImageView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  // We want a square view.
  @SuppressWarnings("SuspiciousNameCombination")
  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, widthMeasureSpec);
  }
}
