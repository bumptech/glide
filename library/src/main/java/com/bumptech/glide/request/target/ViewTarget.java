package com.bumptech.glide.request.target;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
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
 *     To detect {@link View} reuse in {@link android.widget.ListView} or any {@link android.view.ViewGroup} that reuses
 *     views, this class uses the {@link View#setTag(Object)} method to store some metadata so that if a view is reused,
 *     any previous loads or resources from previous loads can be cancelled or reused.
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
     * {@link LayoutParams}. If one or both of the params width and height are less than or
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
        // Some negative sizes (WRAP_CONTENT) are valid, 0 is never valid.
        private static final int PENDING_SIZE = 0;

        private final View view;
        private final List<SizeReadyCallback> cbs = new ArrayList<SizeReadyCallback>();

        private SizeDeterminerLayoutListener layoutListener;
        private Point displayDimens;

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

            int currentWidth = getViewWidthOrParam();
            int currentHeight = getViewHeightOrParam();
            if (!isSizeValid(currentWidth) || !isSizeValid(currentHeight)) {
                return;
            }

            notifyCbs(currentWidth, currentHeight);
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

        public void getSize(SizeReadyCallback cb) {
            int currentWidth = getViewWidthOrParam();
            int currentHeight = getViewHeightOrParam();
            if (isSizeValid(currentWidth) && isSizeValid(currentHeight)) {
                cb.onSizeReady(currentWidth, currentHeight);
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

        private int getViewHeightOrParam() {
            final LayoutParams layoutParams = view.getLayoutParams();
            if (isSizeValid(view.getHeight())) {
                return view.getHeight();
            } else if (layoutParams != null) {
                return getSizeForParam(view, layoutParams.height, true /*isHeight*/);
            } else {
                return PENDING_SIZE;
            }
        }

        private int getViewWidthOrParam() {
            final LayoutParams layoutParams = view.getLayoutParams();
            if (isSizeValid(view.getWidth())) {
                return view.getWidth();
            } else if (layoutParams != null) {
                return getSizeForParam(view, layoutParams.width, false /*isHeight*/);
            } else {
                return PENDING_SIZE;
            }
        }

        private static int getDimensionByParent(View v, boolean isHeight) {
            if (v.getParent() instanceof View) {
                View parent = (View)v.getParent();
                LayoutParams pParams = parent.getLayoutParams();
                if (pParams != null) {
                    int pDimen = isHeight ? parent.getHeight() : parent.getWidth();
                    int ppDimen = isHeight ? pParams.height : pParams.width;
                    if (pDimen > 0 || ppDimen > 0) {
                        return pDimen > 0 ? pDimen : ppDimen;
                    }
                    if (pParams.width == LayoutParams.MATCH_PARENT) {
                        return getDimensionByParent(parent, isHeight);
                    }
                }
            }
            return PENDING_SIZE;
        }

        private int getSizeForParam(View v, int param, boolean isHeight) {
            if (param == LayoutParams.WRAP_CONTENT) {
                Point displayDimens = getDisplayDimens();
                return isHeight ? displayDimens.y : displayDimens.x;
            } else {
                return getDimensionByParent(v, isHeight);
            }
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
        @SuppressWarnings("deprecation")
        private Point getDisplayDimens() {
            if (displayDimens != null) {
                return displayDimens;
            }
            WindowManager windowManager = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                displayDimens = new Point();
                display.getSize(displayDimens);
            } else {
                displayDimens = new Point(display.getWidth(), display.getHeight());
            }
            return displayDimens;
        }

        private boolean isSizeValid(int size) {
            return size > 0 || size == LayoutParams.WRAP_CONTENT || size == LayoutParams.MATCH_PARENT;
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
