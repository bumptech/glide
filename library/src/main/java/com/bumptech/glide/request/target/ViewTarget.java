package com.bumptech.glide.request.target;

import android.content.Context;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import com.bumptech.glide.request.Request;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * A base {@link Target} for loading {@link android.graphics.Bitmap}s into {@link View}s that provides default
 * implementations for most most methods and can determine the size of views using a
 * {@link android.view.ViewTreeObserver.OnDrawListener}.
 *
 * <p>
 *     To detect {@link View} reuse in {@link android.widget.ListView} or any {@link ViewGroup} that reuses views, this
 *     class uses the {@link View#setTag(Object)} method to store some metadata so that if a view is reused, any
 *     previous loads or resources from previous loads can be cancelled or reused.
 * </p>
 *
 * <p>
 *     Any calls to {@link View#setTag(Object)}} on a View given to this class will result in excessive allocations and
 *     and/or {@link IllegalArgumentException}s. If you must call {@link View#setTag(Object)} on a view, consider
 *     using {@link BaseTarget} or {@link SimpleTarget} instead.
 * </p>
 *
 * @param <T> The specific subclass of view wrapped by this target.
 * @param <Z> The resource type this target will receive.
 */
public abstract class ViewTarget<T extends View, Z> extends BaseTarget<Z> {
    private static final String TAG = "ViewTarget";

    protected final T view;
    private final SizeDeterminer sizeDeterminer;

    public ViewTarget(T view) {
        if (view == null) {
            throw new NullPointerException("View must not be null!");
        }
        this.view = view;
        sizeDeterminer = new SizeDeterminer(view);
    }

    /**
     * Returns the wrapped {@link android.view.View}.
     */
    public T getView() {
        return view;
    }

    /**
     * Determines the size of the view by first checking {@link android.view.View#getWidth()} and
     * {@link android.view.View#getHeight()}. If one or both are zero, it then checks the view's
     * {@link android.view.ViewGroup.LayoutParams}. If one or both of the params width and height are less than or
     * equal to zero, it then adds an {@link android.view.ViewTreeObserver.OnPreDrawListener} which waits until the view
     * has been measured before calling the callback with the view's drawn width and height.
     *
     * @param cb {@inheritDoc}
     */
    @Override
    public void getSize(SizeReadyCallback cb) {
        sizeDeterminer.getSize(cb);
    }

    /**
     * Stores the request using {@link View#setTag(Object)}.
     *
     * @param request {@inheritDoc}
     */
    @Override
    public void setRequest(Request request) {
        view.setTag(request);
    }

    /**
     * Returns any stored request using {@link android.view.View#getTag()}.
     *
     * <p>
     *     For Glide to function correctly, Glide must be the only thing that calls {@link View#setTag(Object)}. If the
     *     tag is cleared or set to another object type, Glide will not be able to retrieve and cancel previous loads
     *     which will not only prevent Glide from reusing resource, but will also result in incorrect images being
     *     loaded and lots of flashing of images in lists. As a result, this will throw an
     *     {@link java.lang.IllegalArgumentException} if {@link android.view.View#getTag()}} returns a non null object
     *     that is not an {@link com.bumptech.glide.request.Request}.
     * </p>
     */
    @Override
    public Request getRequest() {
        Object tag = view.getTag();
        Request request = null;
        if (tag != null) {
            if (tag instanceof Request) {
                request = (Request) tag;
            } else {
                throw new IllegalArgumentException("You must not call setTag() on a view Glide is targeting");
            }
        }
        return request;
    }

    @Override
    public String toString() {
        return "Target for: " + view;
    }

    private static class SizeDeterminer {
        private final View view;
        private final List<SizeReadyCallback> cbs = new ArrayList<SizeReadyCallback>();
        private SizeDeterminerLayoutListener layoutListener;

        public SizeDeterminer(View view) {
            this.view = view;
        }

        private void notifyCbs(int width, int height) {
            for (SizeReadyCallback cb : cbs) {
                cb.onSizeReady(width, height);
            }
            cbs.clear();
        }

        private void checkCurrentDimens() {
            if (cbs.isEmpty()) {
                return;
            }

            boolean calledCallback = true;
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            if (isViewSizeValid()) {
                notifyCbs(view.getWidth(), view.getHeight());
            } else if (isLayoutParamsSizeValid()) {
                notifyCbs(layoutParams.width, layoutParams.height);
            } else {
                calledCallback = false;
            }

            if (calledCallback) {
                // Keep a reference to the layout listener and remove it here
                // rather than having the observer remove itself because the observer
                // we add the listener to will be almost immediately merged into
                // another observer and will therefore never be alive. If we instead
                // keep a reference to the listener and remove it here, we get the
                // current view tree observer and should succeed.
                ViewTreeObserver observer = view.getViewTreeObserver();
                if (observer.isAlive()) {
                    observer.removeOnPreDrawListener(layoutListener);
                }
                layoutListener = null;
            }
        }

        public void getSize(SizeReadyCallback cb) {
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            if (isViewSizeValid()) {
                cb.onSizeReady(view.getWidth(), view.getHeight());
            } else if (isLayoutParamsSizeValid()) {
                cb.onSizeReady(layoutParams.width, layoutParams.height);
            } else if (isUsingWrapContent()) {
                WindowManager windowManager =
                        (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
                Display display = windowManager.getDefaultDisplay();
                @SuppressWarnings("deprecation") final int width = display.getWidth(), height = display.getHeight();
                if (Log.isLoggable(TAG, Log.WARN)) {
                    Log.w(TAG, "Trying to load image into ImageView using WRAP_CONTENT, defaulting to screen"
                            + " dimensions: [" + width + "x" + height + "]. Give the view an actual width and height "
                            + " for better performance.");
                }
                cb.onSizeReady(width, height);
            } else {
                // We want to notify callbacks in the order they were added and we only expect one or two callbacks to
                // be added a time, so a List is a reasonable choice.
                if (!cbs.contains(cb)) {
                    cbs.add(cb);
                }
                if (layoutListener == null) {
                    final ViewTreeObserver observer = view.getViewTreeObserver();
                    layoutListener = new SizeDeterminerLayoutListener(this);
                    observer.addOnPreDrawListener(layoutListener);
                }
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
            return layoutParams != null && layoutParams.width > 0 && layoutParams.height > 0;
        }

        private static class SizeDeterminerLayoutListener implements ViewTreeObserver.OnPreDrawListener {
            private final WeakReference<SizeDeterminer> sizeDeterminerRef;

            public SizeDeterminerLayoutListener(SizeDeterminer sizeDeterminer) {
                sizeDeterminerRef = new WeakReference<SizeDeterminer>(sizeDeterminer);
            }

            @Override
            public boolean onPreDraw() {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "OnGlobalLayoutListener called listener=" + this);
                }
                SizeDeterminer sizeDeterminer = sizeDeterminerRef.get();
                if (sizeDeterminer != null) {
                    sizeDeterminer.checkCurrentDimens();
                }
                return true;
            }
        }
    }
}
