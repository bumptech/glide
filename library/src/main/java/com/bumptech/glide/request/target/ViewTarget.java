package com.bumptech.glide.request.target;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Synthetic;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * A base {@link Target} for loading {@link android.graphics.Bitmap}s into {@link View}s that
 * provides default implementations for most most methods and can determine the size of views using
 * a {@link android.view.ViewTreeObserver.OnDrawListener}.
 *
 * <p> To detect {@link View} reuse in {@link android.widget.ListView} or any {@link
 * android.view.ViewGroup} that reuses views, this class uses the {@link View#setTag(Object)} method
 * to store some metadata so that if a view is reused, any previous loads or resources from previous
 * loads can be cancelled or reused. </p>
 *
 * <p> Any calls to {@link View#setTag(Object)}} on a View given to this class will result in
 * excessive allocations and and/or {@link IllegalArgumentException}s. If you must call {@link
 * View#setTag(Object)} on a view, consider using {@link BaseTarget} or {@link SimpleTarget}
 * instead. </p>
 *
 * <p> Subclasses must call super in {@link #onLoadCleared(Drawable)} </p>
 *
 * @param <T> The specific subclass of view wrapped by this target.
 * @param <Z> The resource type this target will receive.
 */
public abstract class ViewTarget<T extends View, Z> extends BaseTarget<Z> {
  private static final String TAG = "ViewTarget";
  private static boolean isTagUsedAtLeastOnce = false;
  @Nullable private static Integer tagId = null;

  protected final T view;
  private final SizeDeterminer sizeDeterminer;

  public ViewTarget(T view) {
    this.view = Preconditions.checkNotNull(view);
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
   * {@link LayoutParams}. If one or both of the params width and height are less than or equal to
   * zero, it then adds an {@link android.view.ViewTreeObserver.OnPreDrawListener} which waits until
   * the view has been measured before calling the callback with the view's drawn width and height.
   *
   * @param cb {@inheritDoc}
   */
  @Override
  public void getSize(SizeReadyCallback cb) {
    sizeDeterminer.getSize(cb);
  }

  @Override
  public void onLoadCleared(Drawable placeholder) {
    super.onLoadCleared(placeholder);
    sizeDeterminer.clearCallbacksAndListener();
  }

  /**
   * Stores the request using {@link View#setTag(Object)}.
   *
   * @param request {@inheritDoc}
   */
  @Override
  public void setRequest(@Nullable Request request) {
    setTag(request);
  }

  /**
   * Returns any stored request using {@link android.view.View#getTag()}.
   *
   * <p> For Glide to function correctly, Glide must be the only thing that calls {@link
   * View#setTag(Object)}. If the tag is cleared or put to another object type, Glide will not be
   * able to retrieve and cancel previous loads which will not only prevent Glide from reusing
   * resource, but will also result in incorrect images being loaded and lots of flashing of images
   * in lists. As a result, this will throw an {@link java.lang.IllegalArgumentException} if {@link
   * android.view.View#getTag()}} returns a non null object that is not an {@link
   * com.bumptech.glide.request.Request}. </p>
   */
  @Override
  @Nullable
  public Request getRequest() {
    Object tag = getTag();
    Request request = null;
    if (tag != null) {
      if (tag instanceof Request) {
        request = (Request) tag;
      } else {
        throw new IllegalArgumentException(
            "You must not call setTag() on a view Glide is targeting");
      }
    }
    return request;
  }

  @Override
  public String toString() {
    return "Target for: " + view;
  }

  private void setTag(@Nullable Object tag) {
    if (tagId == null) {
      isTagUsedAtLeastOnce = true;
      view.setTag(tag);
    } else {
      view.setTag(tagId, tag);
    }
  }

  @Nullable
  private Object getTag() {
    if (tagId == null) {
      return view.getTag();
    } else {
      return view.getTag(tagId);
    }
  }

  /**
   * Sets the android resource id to use in conjunction with {@link View#setTag(int, Object)}
   * to store temporary state allowing loads to be automatically cancelled and resources re-used
   * in scrolling lists.
   *
   * <p>
   *   If no tag id is set, Glide will use {@link View#setTag(Object)}.
   * </p>
   *
   * <p>
   *   Warning: prior to Android 4.0 tags were stored in a static map. Using this method prior
   *   to Android 4.0 may cause memory leaks and isn't recommended. If you do use this method
   *   on older versions, be sure to call {@link com.bumptech.glide.RequestManager#clear(View)} on
   *   any view you start a load into to ensure that the static state is removed.
   * </p>
   *
   * @param tagId The android resource to use.
   */
  public static void setTagId(int tagId) {
      if (ViewTarget.tagId != null || isTagUsedAtLeastOnce) {
          throw new IllegalArgumentException("You cannot set the tag id more than once or change"
              + " the tag id after the first request has been made");
      }
      ViewTarget.tagId = tagId;
  }

  private static class SizeDeterminer {
    // Some negative sizes (WRAP_CONTENT) are valid, 0 is never valid.
    private static final int PENDING_SIZE = 0;
    private final View view;
    private final List<SizeReadyCallback> cbs = new ArrayList<>();

    @Nullable private SizeDeterminerLayoutListener layoutListener;
    @Nullable private Point displayDimens;

    public SizeDeterminer(View view) {
      this.view = view;
    }

    private void notifyCbs(int width, int height) {
      for (SizeReadyCallback cb : cbs) {
        cb.onSizeReady(width, height);
      }
    }

    @Synthetic
    void checkCurrentDimens() {
      if (cbs.isEmpty()) {
        return;
      }

      int currentWidth = getViewWidthOrParam();
      int currentHeight = getViewHeightOrParam();
      if (!isSizeValid(currentWidth) || !isSizeValid(currentHeight)) {
        return;
      }

      notifyCbs(currentWidth, currentHeight);
      clearCallbacksAndListener();
    }

    void getSize(SizeReadyCallback cb) {
      int currentWidth = getViewWidthOrParam();
      int currentHeight = getViewHeightOrParam();
      if (isSizeValid(currentWidth) && isSizeValid(currentHeight)) {
        int paddingAdjustedWidth = currentWidth == WindowManager.LayoutParams.WRAP_CONTENT
            ? currentWidth
            : currentWidth - ViewCompat.getPaddingStart(view) - ViewCompat.getPaddingEnd(view);
        int paddingAdjustedHeight = currentHeight == LayoutParams.WRAP_CONTENT
            ? currentHeight
            : currentHeight - view.getPaddingTop() - view.getPaddingBottom();
        cb.onSizeReady(paddingAdjustedWidth, paddingAdjustedHeight);
      } else {
        // We want to notify callbacks in the order they were added and we only expect one or two
        // callbacks to
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

    void clearCallbacksAndListener() {
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
      cbs.clear();
    }

    private int getViewHeightOrParam() {
      final LayoutParams layoutParams = view.getLayoutParams();
      if (isSizeValid(view.getHeight())) {
        return view.getHeight();
      } else if (layoutParams != null) {
        return getSizeForParam(layoutParams.height, true /*isHeight*/);
      } else {
        return PENDING_SIZE;
      }
    }

    private int getViewWidthOrParam() {
      final LayoutParams layoutParams = view.getLayoutParams();
      if (isSizeValid(view.getWidth())) {
        return view.getWidth();
      } else if (layoutParams != null) {
        return getSizeForParam(layoutParams.width, false /*isHeight*/);
      } else {
        return PENDING_SIZE;
      }
    }

    private int getSizeForParam(int param, boolean isHeight) {
      if (param == LayoutParams.WRAP_CONTENT) {
        Point displayDimens = getDisplayDimens();
        return isHeight ? displayDimens.y : displayDimens.x;
      } else {
        return param;
      }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    @SuppressWarnings("deprecation")
    private Point getDisplayDimens() {
      if (displayDimens != null) {
        return displayDimens;
      }
      WindowManager windowManager =
          (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
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
      return size > 0 || size == LayoutParams.WRAP_CONTENT;
    }

    private static class SizeDeterminerLayoutListener implements ViewTreeObserver
        .OnPreDrawListener {
      private final WeakReference<SizeDeterminer> sizeDeterminerRef;

      public SizeDeterminerLayoutListener(SizeDeterminer sizeDeterminer) {
        sizeDeterminerRef = new WeakReference<>(sizeDeterminer);
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
