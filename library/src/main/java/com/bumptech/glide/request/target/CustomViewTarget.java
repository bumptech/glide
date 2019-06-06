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
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.bumptech.glide.R;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Synthetic;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * A base {@link Target} for loading resources ({@link android.graphics.Bitmap}, {@link Drawable}
 * etc) into {@link View}s that provides default implementations for most methods and can determine
 * the size of views using a {@link android.view.ViewTreeObserver.OnDrawListener}.
 *
 * @param <T> The specific subclass of view wrapped by this target (e.g. {@link
 *     android.widget.ImageView})
 * @param <Z> The resource type this target will receive (e.g. {@link android.graphics.Bitmap}).
 */
public abstract class CustomViewTarget<T extends View, Z> implements Target<Z> {
  private static final String TAG = "CustomViewTarget";
  @IdRes private static final int VIEW_TAG_ID = R.id.glide_custom_view_target_tag;

  private final SizeDeterminer sizeDeterminer;

  protected final T view;
  @Nullable private OnAttachStateChangeListener attachStateListener;
  private boolean isClearedByUs;
  private boolean isAttachStateListenerAdded;

  /** Constructor that defaults {@code waitForLayout} to {@code false}. */
  public CustomViewTarget(@NonNull T view) {
    this.view = Preconditions.checkNotNull(view);
    sizeDeterminer = new SizeDeterminer(view);
  }

  /**
   * A required callback invoked when the resource is no longer valid and must be freed.
   *
   * <p>You must ensure that any current Drawable received in {@link #onResourceReady(Object,
   * Transition)} is no longer used before redrawing the container (usually a View) or changing its
   * visibility. <b>Not doing so will result in crashes in your app.</b>
   *
   * @param placeholder The placeholder drawable to optionally show, or null.
   */
  protected abstract void onResourceCleared(@Nullable Drawable placeholder);

  /**
   * An optional callback invoked when a resource load is started.
   *
   * @see Target#onLoadStarted(Drawable)
   * @param placeholder The placeholder drawable to optionally show, or null.
   */
  protected void onResourceLoading(@Nullable Drawable placeholder) {
    // Default empty.
  }

  @Override
  public void onStart() {
    // Default empty.
  }

  @Override
  public void onStop() {
    // Default empty.
  }

  @Override
  public void onDestroy() {
    // Default empty.
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
  public final CustomViewTarget<T, Z> waitForLayout() {
    sizeDeterminer.waitForLayout = true;
    return this;
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
  public final CustomViewTarget<T, Z> clearOnDetach() {
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

  /**
   * Override the android resource id to store temporary state allowing loads to be automatically
   * cancelled and resources re-used in scrolling lists.
   *
   * <p>Unlike {@link ViewTarget}, it is <b>not</b> necessary to set a custom tag id if your app
   * uses {@link View#setTag(Object)}. It is only necessary if loading several Glide resources into
   * the same view, for example one foreground and one background view.
   *
   * @param tagId The android resource id to use.
   * @deprecated Using this method prevents clearing the target from working properly. Glide uses
   *     its own internal tag id so this method should not be necessary. This method is currently a
   *     no-op.
   */
  // Public API.
  @SuppressWarnings({"UnusedReturnValue", "WeakerAccess"})
  @Deprecated
  public final CustomViewTarget<T, Z> useTagId(@IdRes int tagId) {
    return this;
  }

  /** Returns the wrapped {@link android.view.View}. */
  @NonNull
  public final T getView() {
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
  public final void getSize(@NonNull SizeReadyCallback cb) {
    sizeDeterminer.getSize(cb);
  }

  @Override
  public final void removeCallback(@NonNull SizeReadyCallback cb) {
    sizeDeterminer.removeCallback(cb);
  }

  @Override
  public final void onLoadStarted(@Nullable Drawable placeholder) {
    maybeAddAttachStateListener();
    onResourceLoading(placeholder);
  }

  @Override
  public final void onLoadCleared(@Nullable Drawable placeholder) {
    sizeDeterminer.clearCallbacksAndListener();

    onResourceCleared(placeholder);
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
  public final void setRequest(@Nullable Request request) {
    setTag(request);
  }

  /** Returns any stored request using {@link android.view.View#getTag()}. */
  @Override
  @Nullable
  public final Request getRequest() {
    Object tag = getTag();
    if (tag != null) {
      if (tag instanceof Request) {
        return (Request) tag;
      } else {
        throw new IllegalArgumentException("You must not pass non-R.id ids to setTag(id)");
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return "Target for: " + view;
  }

  @SuppressWarnings("WeakerAccess")
  @Synthetic
  final void resumeMyRequest() {
    Request request = getRequest();
    if (request != null && request.isCleared()) {
      request.begin();
    }
  }

  @SuppressWarnings("WeakerAccess")
  @Synthetic
  final void pauseMyRequest() {
    Request request = getRequest();
    if (request != null) {
      isClearedByUs = true;
      request.clear();
      isClearedByUs = false;
    }
  }

  private void setTag(@Nullable Object tag) {
    view.setTag(VIEW_TAG_ID, tag);
  }

  @Nullable
  private Object getTag() {
    return view.getTag(VIEW_TAG_ID);
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
              "Glide treats LayoutParams.WRAP_CONTENT as a request for an image the size of"
                  + " this device's screen dimensions. If you want to load the original image and"
                  + " are ok with the corresponding memory cost and OOMs (depending on the input"
                  + " size), use .override(Target.SIZE_ORIGINAL). Otherwise, use"
                  + " LayoutParams.MATCH_PARENT, set layout_width and layout_height to fixed"
                  + " dimension, or use .override() with fixed dimensions.");
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
