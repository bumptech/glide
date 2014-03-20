package com.bumptech.glide.presenter.target;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.ImageView;
import com.bumptech.glide.presenter.ImagePresenter;

import java.lang.ref.WeakReference;

import static android.view.ViewGroup.LayoutParams;

/**
 * A target wrapping an ImageView. Obtains the runtime dimensions of the ImageView.
 */
public class ImageViewTarget implements Target {
    private static final String TAG = "ImageViewTarget";
    private final ImageView imageView;
    private final SizeDeterminer sizeDeterminer;
    private ImagePresenter imagePresenter;

    public ImageViewTarget(ImageView imageView) {
        this.imageView = imageView;
        this.sizeDeterminer = new SizeDeterminer(imageView);
    }

    @Override
    public void onImageReady(Bitmap bitmap) {
        imageView.setImageBitmap(bitmap);
    }

    @Override
    public void setPlaceholder(Drawable placeholder) {
        imageView.setImageDrawable(placeholder);
    }

    @Override
    public void getSize(SizeReadyCallback cb) {
        sizeDeterminer.getSize(cb);
    }

    @Override
    public void startAnimation(Animation animation) {
        imageView.clearAnimation();

        imageView.startAnimation(animation);
    }

    @Override
    public void setImagePresenter(ImagePresenter imagePresenter) {
        this.imagePresenter = imagePresenter;
    }

    @Override
    public ImagePresenter getImagePresenter() {
        return imagePresenter;
    }

    @Override
    public int hashCode() {
        return imageView.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (!(o instanceof ImageViewTarget)) {
            return false;
        }
        ImageViewTarget other = (ImageViewTarget) o;
        return imageView.equals(other.imageView);
    }

    private static class SizeDeterminer {
        private final View view;
        private SizeReadyCallback cb;
        private SizeDeterminerLayoutListener layoutListener;

        private void checkCurrentDimens() {
            if (cb == null) return;

            boolean calledCallback = true;
            LayoutParams layoutParams = view.getLayoutParams();
            if (isViewSizeValid()) {
                cb.onSizeReady(view.getWidth(), view.getHeight());
            } else if (isLayoutParamsSizeValid()) {
                cb.onSizeReady(layoutParams.width, layoutParams.height);
            } else {
                calledCallback = false;
            }

            if (calledCallback) {
                cb = null;
                // Keep a reference to the layout listener and remove it here
                // rather than having the observer remove itself because the observer
                // we add the listener to will be almost immediately merged into
                // another observer and will therefore never be alive. If we instead
                // keep a reference to the listener and remove it here, we get the
                // current view tree observer and should succeed.
                ViewTreeObserver observer = view.getViewTreeObserver();
                if (observer.isAlive()) {
                    observer.removeGlobalOnLayoutListener(layoutListener);
                }
            }
        }

        public SizeDeterminer(View view) {
            this.view = view;
        }

        public void getSize(SizeReadyCallback cb) {
            this.cb = null;
            LayoutParams layoutParams = view.getLayoutParams();
            if (isViewSizeValid()) {
                cb.onSizeReady(view.getWidth(), view.getHeight());
            } else if (isLayoutParamsSizeValid()) {
                cb.onSizeReady(layoutParams.width, layoutParams.height);
            } else if (isUsingWrapContent()) {
                WindowManager windowManager =
                        (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
                Display display = windowManager.getDefaultDisplay();
                final int width = display.getWidth();
                final int height = display.getHeight();
                if (Log.isLoggable(TAG, Log.WARN)) {
                    Log.w(TAG, "Trying to load image into ImageView using WRAP_CONTENT, defaulting to screen" +
                            " dimensions: [" + width + "x" + height + "]. Give the view an actual width and height " +
                            " for better performance.");
                }
                cb.onSizeReady(display.getWidth(), display.getHeight());
            } else {
                this.cb = cb;
                final ViewTreeObserver observer = view.getViewTreeObserver();
                layoutListener = new SizeDeterminerLayoutListener(this);
                observer.addOnGlobalLayoutListener(layoutListener);
            }
        }

        private boolean isViewSizeValid() {
            return view.getWidth() > 0 && view.getHeight() > 0;
        }

        private boolean isUsingWrapContent() {
            final LayoutParams layoutParams = view.getLayoutParams();
            return layoutParams != null && (layoutParams.width == LayoutParams.WRAP_CONTENT
                    || layoutParams.height == LayoutParams.WRAP_CONTENT);
        }

        private boolean isLayoutParamsSizeValid() {
            final LayoutParams layoutParams = view.getLayoutParams();
            return layoutParams != null && (layoutParams.width > 0 && layoutParams.height > 0);
        }

        private static class SizeDeterminerLayoutListener implements ViewTreeObserver.OnGlobalLayoutListener {
            private final WeakReference<SizeDeterminer> sizeDeterminerRef;

            public SizeDeterminerLayoutListener(SizeDeterminer sizeDeterminer) {
                sizeDeterminerRef = new WeakReference<SizeDeterminer>(sizeDeterminer);
            }

            @Override
            public void onGlobalLayout() {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "OnGlobalLayoutListener called listener=" + this);
                }
                SizeDeterminer sizeDeterminer = sizeDeterminerRef.get();
                if (sizeDeterminer != null) {
                    sizeDeterminer.checkCurrentDimens();
                }
            }
        }
    }
}
