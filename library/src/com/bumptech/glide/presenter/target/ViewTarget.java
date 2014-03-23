package com.bumptech.glide.presenter.target;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.ListView;
import com.bumptech.glide.presenter.ImagePresenter;

import java.lang.ref.WeakReference;

/**
 * A base {@link Target} for loading {@link Bitmap}s into {@link View}s that provides default implementations for most
 * most methods and can determine the size of views using a {@link ViewTreeObserver.OnGlobalLayoutListener}.
 *
 * <p>
 *     To detect {@link View} reuse in {@link ListView} or any {@link ViewGroup} that reuses views, this class uses the
 *     {@link View#setTag(Object)} method to store some metadata so that if a view is reused, any previous loads or
 *     resources from previous loads can be cancelled or reused.
 * </p>
 *
 * <p>
 *     Any calls to {@link View#setTag(Object)}} on a View given to this class will result in excessive allocations and
 *     and/or {@link IllegalArgumentException}s. If you must call {@link View#setTag(Object)} on a view, consider
 *     using {@link BaseTarget} or {@link SimpleTarget} instead.
 * </p>
 *
 * @param <T> The specific subclass of view wrapped by this target.
 */
public abstract class ViewTarget<T extends View> implements Target {
    private static final String TAG = "ViewTarget";
    private final T view;
    private final SizeDeterminer sizeDeterminer;

    public ViewTarget(T view) {
        this.view = view;
        sizeDeterminer = new SizeDeterminer(view);
    }

    public T getView() {
        return view;
    }

    @Override
    public void getSize(SizeReadyCallback cb) {
        sizeDeterminer.getSize(cb);
    }

    @Override
    public void startAnimation(Animation animation) {
        view.clearAnimation();

        view.startAnimation(animation);
    }

    @Override
    public void setImagePresenter(ImagePresenter imagePresenter) {
        view.setTag(imagePresenter);
    }

    @Override
    public ImagePresenter getImagePresenter() {
        Object tag = view.getTag();
        ImagePresenter presenter = null;
        if (tag != null) {
            if ((tag instanceof ImagePresenter)) {
                presenter = (ImagePresenter) tag;
            } else {
                throw new IllegalArgumentException("You must not call setTag() on a view Glide is targeting");
            }
        }
        return presenter;
    }

    private static class SizeDeterminer {
        private final View view;
        private SizeReadyCallback cb;
        private SizeDeterminerLayoutListener layoutListener;

        public SizeDeterminer(View view) {
            this.view = view;
        }

        private void checkCurrentDimens() {
            if (cb == null) return;

            boolean calledCallback = true;
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
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

        public void getSize(SizeReadyCallback cb) {
            this.cb = null;
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
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
            final ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            return layoutParams != null && (layoutParams.width == ViewGroup.LayoutParams.WRAP_CONTENT
                    || layoutParams.height == ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        private boolean isLayoutParamsSizeValid() {
            final ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
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
