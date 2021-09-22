package com.bumptech.glide.request.target;

import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.bumptech.glide.R;
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
 * <p>To detect {@link View} reuse in {@link android.widget.ListView} or any {@link
 * android.view.ViewGroup} that reuses views, this class uses the {@link View#setTag(Object)} method
 * to store some metadata so that if a view is reused, any previous loads or resources from previous
 * loads can be cancelled or reused.
 *
 * <p>Any calls to {@link View#setTag(Object)}} on a View given to this class will result in
 * excessive allocations and and/or {@link IllegalArgumentException}s. If you must call {@link
 * View#setTag(Object)} on a view, use {@link #setTagId(int)} to specify a custom tag for Glide to
 * use.
 *
 * <p>Subclasses must call super in {@link #onLoadCleared(Drawable)}
 *
 * @param <T> The specific subclass of view wrapped by this target.
 * @param <Z> The resource type this target will receive.
 * @deprecated Use {@link CustomViewTarget}. Using this class is unsafe without implementing {@link
 *     #onLoadCleared} and results in recycled bitmaps being referenced from the UI and hard to
 *     root-cause crashes.
 */
@Deprecated
public abstract class ViewTarget<T extends View, Z> extends BaseTarget<Z> {
  private static final String TAG = "ViewTarget";
  private static boolean isTagUsedAtLeastOnce;
  private static int tagId = R.id.glide_custom_view_target_tag;

  protected final T view;
  private final SizeDeterminer sizeDeterminer;
  @Nullable private OnAttachStateChangeListener attachStateListener;
  private boolean isClearedByUs;
  private boolean isAttachStateListenerAdded;

  /** Constructor that defaults {@code waitForLayout} to {@code false}. */
  public ViewTarget(@NonNull T view) {
    this.view = Preconditions.checkNotNull(view);
    sizeDeterminer = new SizeDeterminer(view);
  }

  /**
   * @param waitForLayout If set to {@code true}, Glide will always wait for any pending layout pass
   *     before checking for the size a View. If set to {@code false} Glide will only wait for a
   *     pending layout pass if it's unable to resolve the size from layout parameters or an
   *     existing View size. Because setting this parameter to {@code true} forces Glide to wait for
   *     the layout pass to occur before starting the load, setting this parameter to {@code true}
   *     can cause flashing in some cases and should be used sparingly. If layout parameters are set
   *     to fixed sizes, they will still be used instead of the View's dimensions even if this
   *     parameter is set to {@code true}. This parameter is a fallback only.
   * @deprecated Use {@link #waitForLayout()} instead.
   */
  @SuppressWarnings("WeakerAccess") // Public API
  @Deprecated
  public ViewTarget(@NonNull T view, boolean waitForLayout) {
    this(view);
    if (waitForLayout) {
      waitForLayout();
    }
  }

  /**
   * Clears the {@link View}'s {@link Request} when the {@link View} is detached from its {@link
   * android.view.Window} and restarts the {@link Request} when the {@link View} is re-attached from
   * its {@link android.view.Window}.
   *
   * <p>This is an experimental API that may be removed in a future version.
   *
   * <p>Using this method can save memory by allowing Glide to more eagerly clear resources when
   * transitioning screens or swapping adapters in scrolling views. However it also substantially
   * increases the odds that images will not be in memory if users subsequently return to a screen
   * where images were previously loaded. Whether or not this happens will depend on the number of
   * images loaded in the new screen and the size of the memory cache. Increasing the size of the
   * memory cache can improve this behavior but it largely negates the memory benefits of using this
   * method.
   *
   * <p>Use this method with caution and measure your memory usage to ensure that it's actually
   * improving your memory usage in the cases you care about.
   */
  // Public API.
  @NonNull
  @SuppressWarnings({"UnusedReturnValue", "WeakerAccess"})
  public final ViewTarget<T, Z> clearOnDetach() {
    if (attachStateListener != null) {
      return this;
    }
    attachStateListener =
        new OnAttachStateChangeListener() {
          @Override
          public void onViewAttachedToWindow(View v) {
            resumeMyRequest();
          }

          @Override
          public void onViewDetachedFromWindow(View v) {
            pauseMyRequest();
          }
        };
    maybeAddAttachStateListener();
    return this;
  }

  @SuppressWarnings("WeakerAccess")
  @Synthetic
  void resumeMyRequest() {
    Request request = getRequest();
    if (request != null && request.isCleared()) {
      request.begin();
    }
  }

  @SuppressWarnings("WeakerAccess")
  @Synthetic
  void pauseMyRequest() {
    Request request = getRequest();
    // If the Request were cleared by the developer, it would be null here. The only way it's
    // present is if the developer hasn't previously cleared this Target.
    if (request != null) {
      isClearedByUs = true;
      request.clear();
      isClearedByUs = false;
    }
  }

  /**
   * Indicates that Glide should always wait for any pending layout pass before checking for the
   * size an {@link View}.
   *
   * <p>By default, Glide will only wait for a pending layout pass if it's unable to resolve the
   * size from the {@link LayoutParams} or valid non-zero values for {@link View#getWidth()} and
   * {@link View#getHeight()}.
   *
   * <p>Because calling this method forces Glide to wait for the layout pass to occur before
   * starting loads, setting this parameter to {@code true} can cause Glide to asynchronous load an
   * image even if it's in the memory cache. The load will happen asynchronously because Glide has
   * to wait for a layout pass to occur, which won't necessarily happen in the same frame as when
   * the image is requested. As a result, using this method can resulting in flashing in some cases
   * and should be used sparingly.
   *
   * <p>If the {@link LayoutParams} of the wrapped {@link View} are set to fixed sizes, they will
   * still be used instead of the {@link View}'s dimensions even if this method is called. This
   * parameter is a fallback only.
   */
  @SuppressWarnings("WeakerAccess") // Public API
  @NonNull
  public final ViewTarget<T, Z> waitForLayout() {
    sizeDeterminer.waitForLayout = true;
    return this;
  }

  @CallSuper
  @Override
  public void onLoadStarted(@Nullable Drawable placeholder) {
    super.onLoadStarted(placeholder);
    maybeAddAttachStateListener();
  }

  private void maybeAddAttachStateListener() {
    if (attachStateListener == null || isAttachStateListenerAdded) {
      return;
    }

    view.addOnAttachStateChangeListener(attachStateListener);
    isAttachStateListenerAdded = true;
  }

  private void maybeRemoveAttachStateListener() {
    if (attachStateListener == null || !isAttachStateListenerAdded) {
      return;
    }

    view.removeOnAttachStateChangeListener(attachStateListener);
    isAttachStateListenerAdded = false;
  }

  /** Returns the wrapped {@link android.view.View}. */
  @NonNull
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
  @CallSuper
  @Override
  public void getSize(@NonNull SizeReadyCallback cb) {
    sizeDeterminer.getSize(cb);
  }

  @CallSuper
  @Override
  public void removeCallback(@NonNull SizeReadyCallback cb) {
    sizeDeterminer.removeCallback(cb);
  }

  @CallSuper
  @Override
  public void onLoadCleared(@Nullable Drawable placeholder) {
    super.onLoadCleared(placeholder);
    sizeDeterminer.clearCallbacksAndListener();

    if (!isClearedByUs) {
      maybeRemoveAttachStateListener();
    }
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
   * <p>For Glide to function correctly, Glide must be the only thing that calls {@link
   * View#setTag(Object)}. If the tag is cleared or put to another object type, Glide will not be
   * able to retrieve and cancel previous loads which will not only prevent Glide from reusing
   * resource, but will also result in incorrect images being loaded and lots of flashing of images
   * in lists. As a result, this will throw an {@link java.lang.IllegalArgumentException} if {@link
   * android.view.View#getTag()}} returns a non null object that is not an {@link
   * com.bumptech.glide.request.Request}.
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
    isTagUsedAtLeastOnce = true;
    view.setTag(tagId, tag);
  }

  @Nullable
  private Object getTag() {
    return view.getTag(tagId);
  }

  /**
   * Sets the android resource id to use in conjunction with {@link View#setTag(int, Object)} to
   * store temporary state allowing loads to be automatically cancelled and resources re-used in
   * scrolling lists.
   *
   * <p>If no tag id is set, Glide will use {@link View#setTag(Object)}.
   *
   * <p>Warning: prior to Android 4.0 tags were stored in a static map. Using this method prior to
   * Android 4.0 may cause memory leaks and isn't recommended. If you do use this method on older
   * versions, be sure to call {@link com.bumptech.glide.RequestManager#clear(View)} on any view you
   * start a load into to ensure that the static state is removed.
   *
   * @deprecated Glide uses it's own default tag id, so there's no need to specify your own. This
   *     method will be removed in a future version.
   * @param tagId The android resource to use.
   */
  // Public API.
  @SuppressWarnings("unused")
  @Deprecated
  public static void setTagId(int tagId) {
    if (isTagUsedAtLeastOnce) {
      throw new IllegalArgumentException(
          "You cannot set the tag id more than once or change"
              + " the tag id after the first request has been made");
    }
    ViewTarget.tagId = tagId;
  }

  @VisibleForTesting
  static final class SizeDeterminer {
    // Some negative sizes (Target.SIZE_ORIGINAL) are valid, 0 is never valid.
    private static final int PENDING_SIZE = 0;
    @VisibleForTesting @Nullable static Integer maxDisplayLength;
    private final View view;
    private final List<SizeReadyCallback> cbs = new ArrayList<>();
    @Synthetic boolean waitForLayout;

    @Nullable private SizeDeterminerLayoutListener layoutListener;

    SizeDeterminer(@NonNull View view) {
      this.view = view;
    }

    // Use the maximum to avoid depending on the device's current orientation.
    private static int getMaxDisplayLength(@NonNull Context context) {
      if (maxDisplayLength == null) {
        WindowManager windowManager =
            (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = Preconditions.checkNotNull(windowManager).getDefaultDisplay();
        Point displayDimensions = new Point();
        display.getSize(displayDimensions);
        maxDisplayLength = Math.max(displayDimensions.x, displayDimensions.y);
      }
      return maxDisplayLength;
    }

    private void notifyCbs(int width, int height) {
      // One or more callbacks may trigger the removal of one or more additional callbacks, so we
      // need a copy of the list to avoid a concurrent modification exception. One place this
      // happens is when a full request completes from the in memory cache while its thumbnail is
      // still being loaded asynchronously. See #2237.
      for (SizeReadyCallback cb : new ArrayList<>(cbs)) {
        cb.onSizeReady(width, height);
      }
    }

    @Synthetic
    void checkCurrentDimens() {
      if (cbs.isEmpty()) {
        return;
      }

      int currentWidth = getTargetWidth();
      int currentHeight = getTargetHeight();
      if (!isViewStateAndSizeValid(currentWidth, currentHeight)) {
        return;
      }

      notifyCbs(currentWidth, currentHeight);
      clearCallbacksAndListener();
    }

    void getSize(@NonNull SizeReadyCallback cb) {
      int currentWidth = getTargetWidth();
      int currentHeight = getTargetHeight();
      if (isViewStateAndSizeValid(currentWidth, currentHeight)) {
        cb.onSizeReady(currentWidth, currentHeight);
        return;
      }

      // We want to notify callbacks in the order they were added and we only expect one or two
      // callbacks to be added a time, so a List is a reasonable choice.
      if (!cbs.contains(cb)) {
        cbs.add(cb);
      }
      if (layoutListener == null) {
        ViewTreeObserver observer = view.getViewTreeObserver();
        layoutListener = new SizeDeterminerLayoutListener(this);
        observer.addOnPreDrawListener(layoutListener);
      }
    }

    /**
     * The callback may be called anyway if it is removed by another {@link SizeReadyCallback} or
     * otherwise removed while we're notifying the list of callbacks.
     *
     * <p>See #2237.
     */
    void removeCallback(@NonNull SizeReadyCallback cb) {
      cbs.remove(cb);
    }

    void clearCallbacksAndListener() {
      // Keep a reference to the layout attachStateListener and remove it here
      // rather than having the observer remove itself because the observer
      // we add the attachStateListener to will be almost immediately merged into
      // another observer and will therefore never be alive. If we instead
      // keep a reference to the attachStateListener and remove it here, we get the
      // current view tree observer and should succeed.
      ViewTreeObserver observer = view.getViewTreeObserver();
      if (observer.isAlive()) {
        observer.removeOnPreDrawListener(layoutListener);
      }
      layoutListener = null;
      cbs.clear();
    }

    private boolean isViewStateAndSizeValid(int width, int height) {
      return isDimensionValid(width) && isDimensionValid(height);
    }

    private int getTargetHeight() {
      int verticalPadding = view.getPaddingTop() + view.getPaddingBottom();
      LayoutParams layoutParams = view.getLayoutParams();
      int layoutParamSize = layoutParams != null ? layoutParams.height : PENDING_SIZE;
      return getTargetDimen(view.getHeight(), layoutParamSize, verticalPadding);
    }

    private int getTargetWidth() {
      int horizontalPadding = view.getPaddingLeft() + view.getPaddingRight();
      LayoutParams layoutParams = view.getLayoutParams();
      int layoutParamSize = layoutParams != null ? layoutParams.width : PENDING_SIZE;
      return getTargetDimen(view.getWidth(), layoutParamSize, horizontalPadding);
    }

    private int getTargetDimen(int viewSize, int paramSize, int paddingSize) {
      // We consider the View state as valid if the View has non-null layout params and a non-zero
      // layout params width and height. This is imperfect. We're making an assumption that View
      // parents will obey their child's layout parameters, which isn't always the case.
      int adjustedParamSize = paramSize - paddingSize;
      if (adjustedParamSize > 0) {
        return adjustedParamSize;
      }

      // Since we always prefer layout parameters with fixed sizes, even if waitForLayout is true,
      // we might as well ignore it and just return the layout parameters above if we have them.
      // Otherwise we should wait for a layout pass before checking the View's dimensions.
      if (waitForLayout && view.isLayoutRequested()) {
        return PENDING_SIZE;
      }

      // We also consider the View state valid if the View has a non-zero width and height. This
      // means that the View has gone through at least one layout pass. It does not mean the Views
      // width and height are from the current layout pass. For example, if a View is re-used in
      // RecyclerView or ListView, this width/height may be from an old position. In some cases
      // the dimensions of the View at the old position may be different than the dimensions of the
      // View in the new position because the LayoutManager/ViewParent can arbitrarily decide to
      // change them. Nevertheless, in most cases this should be a reasonable choice.
      int adjustedViewSize = viewSize - paddingSize;
      if (adjustedViewSize > 0) {
        return adjustedViewSize;
      }

      // Finally we consider the view valid if the layout parameter size is set to wrap_content.
      // It's difficult for Glide to figure out what to do here. Although Target.SIZE_ORIGINAL is a
      // coherent choice, it's extremely dangerous because original images may be much too large to
      // fit in memory or so large that only a couple can fit in memory, causing OOMs. If users want
      // the original image, they can always use .override(Target.SIZE_ORIGINAL). Since wrap_content
      // may never resolve to a real size unless we load something, we aim for a square whose length
      // is the largest screen size. That way we're loading something and that something has some
      // hope of being downsampled to a size that the device can support. We also log a warning that
      // tries to explain what Glide is doing and why some alternatives are preferable.
      // Since WRAP_CONTENT is sometimes used as a default layout parameter, we always wait for
      // layout to complete before using this fallback parameter (ConstraintLayout among others).
      if (!view.isLayoutRequested() && paramSize == LayoutParams.WRAP_CONTENT) {
        if (Log.isLoggable(TAG, Log.INFO)) {
          Log.i(
              TAG,
              "Glide treats LayoutParams.WRAP_CONTENT as a request for an image the size of this"
                  + " device's screen dimensions. If you want to load the original image and are"
                  + " ok with the corresponding memory cost and OOMs (depending on the input size),"
                  + " use override(Target.SIZE_ORIGINAL). Otherwise, use LayoutParams.MATCH_PARENT,"
                  + " set layout_width and layout_height to fixed dimension, or use .override()"
                  + " with fixed dimensions.");
        }
        return getMaxDisplayLength(view.getContext());
      }

      // If the layout parameters are < padding, the view size is < padding, or the layout
      // parameters are set to match_parent or wrap_content and no layout has occurred, we should
      // wait for layout and repeat.
      return PENDING_SIZE;
    }

    private boolean isDimensionValid(int size) {
      return size > 0 || size == SIZE_ORIGINAL;
    }

    private static final class SizeDeterminerLayoutListener
        implements ViewTreeObserver.OnPreDrawListener {
      private final WeakReference<SizeDeterminer> sizeDeterminerRef;

      SizeDeterminerLayoutListener(@NonNull SizeDeterminer sizeDeterminer) {
        sizeDeterminerRef = new WeakReference<>(sizeDeterminer);
      }

      @Override
      public boolean onPreDraw() {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
          Log.v(TAG, "OnGlobalLayoutListener called attachStateListener=" + this);
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
