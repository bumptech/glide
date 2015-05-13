package com.bumptech.glide.request;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.util.Pools;
import android.support.v4.util.Pools.SimplePool;
import android.util.Log;

import com.bumptech.glide.GlideContext;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.Engine;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.request.transition.TransitionFactory;
import com.bumptech.glide.util.LogTime;
import com.bumptech.glide.util.Util;

/**
 * A {@link Request} that loads a {@link com.bumptech.glide.load.engine.Resource} into a given
 * {@link Target}.
 *
 * @param <R> The type of the resource that will be transcoded from the loaded resource.
 */
public final class SingleRequest<R> implements Request,
    SizeReadyCallback,
    ResourceCallback {
  private static final String TAG = "Request";
  private static final Pools.Pool<SingleRequest<?>> REQUEST_POOL = new SimplePool<>(150);
  private static final double TO_MEGABYTE = 1d / (1024d * 1024d);

  private enum Status {
    /**
     * Created but not yet running.
     */
    PENDING,
    /**
     * In the process of fetching media.
     */
    RUNNING,
    /**
     * Waiting for a callback given to the Target to be called to determine target dimensions.
     */
    WAITING_FOR_SIZE,
    /**
     * Finished loading media successfully.
     */
    COMPLETE,
    /**
     * Failed to load media, may be restarted.
     */
    FAILED,
    /**
     * Cancelled by the user, may not be restarted.
     */
    CANCELLED,
    /**
     * Cleared by the user with a placeholder set, may not be restarted.
     */
    CLEARED,
    /**
     * Temporarily paused by the system, may be restarted.
     */
    PAUSED,
  }

  private final String tag = String.valueOf(hashCode());

  private RequestCoordinator requestCoordinator;
  private GlideContext glideContext;
  private Object model;
  private Class<R> transcodeClass;
  private BaseRequestOptions<?> requestOptions;
  private int overrideWidth;
  private int overrideHeight;
  private Priority priority;
  private Target<R> target;
  private RequestListener<R> requestListener;
  private Engine engine;
  private TransitionFactory<? super R> animationFactory;
  private boolean loadedFromMemoryCache;
  private Resource<R> resource;
  private Engine.LoadStatus loadStatus;
  private long startTime;
  private Status status;
  private Drawable errorDrawable;
  private Drawable placeholderDrawable;
  private Drawable fallbackDrawable;

  public static <R> SingleRequest<R> obtain(
      GlideContext glideContext,
      Object model,
      Class<R> transcodeClass,
      BaseRequestOptions<?> requestOptions,
      int overrideWidth,
      int overrideHeight,
      Priority priority,
      Target<R> target,
      RequestListener<R> requestListener,
      RequestCoordinator requestCoordinator,
      Engine engine,
      TransitionFactory<? super R> animationFactory) {
    @SuppressWarnings("unchecked") SingleRequest<R> request =
        (SingleRequest<R>) REQUEST_POOL.acquire();
    if (request == null) {
      request = new SingleRequest<>();
    }
    request.init(
        glideContext,
        model,
        transcodeClass,
        requestOptions,
        overrideWidth,
        overrideHeight,
        priority,
        target,
        requestListener,
        requestCoordinator,
        engine,
        animationFactory);
    return request;
  }

  private SingleRequest() {
    // just create, instances are reused with recycle/init
  }

  private void init(
      GlideContext glideContext,
      Object model,
      Class<R> transcodeClass,
      BaseRequestOptions<?> requestOptions,
      int overrideWidth,
      int overrideHeight,
      Priority priority,
      Target<R> target,
      RequestListener<R> requestListener,
      RequestCoordinator requestCoordinator,
      Engine engine,
      TransitionFactory<? super R> animationFactory) {
    this.glideContext = glideContext;
    this.model = model;
    this.transcodeClass = transcodeClass;
    this.requestOptions = requestOptions;
    this.overrideWidth = overrideWidth;
    this.overrideHeight = overrideHeight;
    this.priority = priority;
    this.target = target;
    this.requestListener = requestListener;
    this.requestCoordinator = requestCoordinator;
    this.engine = engine;
    this.animationFactory = animationFactory;
    status = Status.PENDING;
  }

  @Override
  public void recycle() {
    glideContext = null;
    model = null;
    transcodeClass = null;
    requestOptions = null;
    overrideWidth = -1;
    overrideHeight = -1;
    target = null;
    requestListener = null;
    requestCoordinator = null;
    animationFactory = null;
    loadedFromMemoryCache = false;
    loadStatus = null;
    errorDrawable = null;
    placeholderDrawable = null;
    fallbackDrawable = null;
    REQUEST_POOL.release(this);
  }

  @Override
  public void begin() {
    startTime = LogTime.getLogTime();
    if (model == null) {
      onLoadFailed(new GlideException("Received null model"));
      return;
    }

    status = Status.WAITING_FOR_SIZE;
    if (Util.isValidDimensions(overrideWidth, overrideHeight)) {
      onSizeReady(overrideWidth, overrideHeight);
    } else {
      target.getSize(this);
    }

    if ((status == Status.RUNNING || status == Status.WAITING_FOR_SIZE)
        && canNotifyStatusChanged()) {
      target.onLoadStarted(getPlaceholderDrawable());
    }
    if (Log.isLoggable(TAG, Log.VERBOSE)) {
      logV("finished run method in " + LogTime.getElapsedMillis(startTime));
    }
  }

  /**
   * Cancels the current load but does not release any resources held by the request and continues
   * to display the loaded resource if the load completed before the call to cancel.
   *
   * <p> Cancelled requests can be restarted with a subsequent call to {@link #begin()}. </p>
   *
   * @see #clear()
   */
  void cancel() {
    status = Status.CANCELLED;
    if (loadStatus != null) {
      loadStatus.cancel();
      loadStatus = null;
    }
  }

  /**
   * Cancels the current load if it is in progress, clears any resources held onto by the request
   * and replaces the loaded resource if the load completed with the placeholder.
   *
   * <p> Cleared requests can be restarted with a subsequent call to {@link #begin()} </p>
   *
   * @see #cancel()
   */
  @Override
  public void clear() {
    Util.assertMainThread();
    if (status == Status.CLEARED) {
      return;
    }
    cancel();
    // Resource must be released before canNotifyStatusChanged is called.
    if (resource != null) {
      releaseResource(resource);
    }
    if (canNotifyStatusChanged()) {
      target.onLoadCleared(getPlaceholderDrawable());
    }
    // Must be after cancel().
    status = Status.CLEARED;
  }

  @Override
  public boolean isPaused() {
    return status == Status.PAUSED;
  }

  @Override
  public void pause() {
    clear();
    status = Status.PAUSED;
  }

  private void releaseResource(Resource resource) {
    engine.release(resource);
    this.resource = null;
  }

  @Override
  public boolean isRunning() {
    return status == Status.RUNNING || status == Status.WAITING_FOR_SIZE;
  }

  @Override
  public boolean isComplete() {
    return status == Status.COMPLETE;
  }

  @Override
  public boolean isResourceSet() {
    return isComplete();
  }

  @Override
  public boolean isCancelled() {
    return status == Status.CANCELLED || status == Status.CLEARED;
  }

  @Override
  public boolean isFailed() {
    return status == Status.FAILED;
  }

  private Drawable getErrorDrawable() {
    if (errorDrawable == null) {
      errorDrawable = requestOptions.getErrorPlaceholder();
      if (errorDrawable == null && requestOptions.getErrorId() > 0) {
        errorDrawable = loadDrawable(requestOptions.getErrorId());
      }
    }
    return errorDrawable;
  }

  private Drawable getPlaceholderDrawable() {
     if (placeholderDrawable == null) {
      placeholderDrawable = requestOptions.getPlaceholderDrawable();
      if (placeholderDrawable == null && requestOptions.getPlaceholderId() > 0) {
        placeholderDrawable = loadDrawable(requestOptions.getPlaceholderId());
      }
    }
    return placeholderDrawable;
  }

  private Drawable getFallbackDrawable() {
    if (fallbackDrawable == null) {
      fallbackDrawable = requestOptions.getFallbackDrawable();
      if (fallbackDrawable == null && requestOptions.getFallbackId() > 0) {
        fallbackDrawable = loadDrawable(requestOptions.getFallbackId());
      }
    }
    return fallbackDrawable;
  }

  private Drawable loadDrawable(int resouceId) {
    Resources resources = glideContext.getResources();
    return ResourcesCompat.getDrawable(resources, resouceId, requestOptions.getTheme());
  }

  private void setErrorPlaceholder() {
    if (!canNotifyStatusChanged()) {
      return;
    }

    Drawable error = model == null ? getFallbackDrawable() : getErrorDrawable();
    if (error == null) {
      error = getPlaceholderDrawable();
    }
    target.onLoadFailed(error);
  }

  /**
   * A callback method that should never be invoked directly.
   */
  @Override
  public void onSizeReady(int width, int height) {
    if (Log.isLoggable(TAG, Log.VERBOSE)) {
      logV("Got onSizeReady in " + LogTime.getElapsedMillis(startTime));
    }
    if (status != Status.WAITING_FOR_SIZE) {
      return;
    }
    status = Status.RUNNING;

    float sizeMultiplier = requestOptions.getSizeMultiplier();
    width = Math.round(sizeMultiplier * width);
    height = Math.round(sizeMultiplier * height);

    if (Log.isLoggable(TAG, Log.VERBOSE)) {
      logV("finished setup for calling load in " + LogTime.getElapsedMillis(startTime));
    }
    loadedFromMemoryCache = true;
    loadStatus = engine.load(
        glideContext,
        model,
        requestOptions.getSignature(),
        width,
        height,
        requestOptions.getResourceClass(),
        transcodeClass,
        priority,
        requestOptions.getDiskCacheStrategy(),
        requestOptions.getTransformations(),
        requestOptions.isTransformationRequired(),
        requestOptions.getOptions(),
        requestOptions.isMemoryCacheable(),
        this);
    loadedFromMemoryCache = resource != null;
    if (Log.isLoggable(TAG, Log.VERBOSE)) {
      logV("finished onSizeReady in " + LogTime.getElapsedMillis(startTime));
    }
  }

  private boolean canSetResource() {
    return requestCoordinator == null || requestCoordinator.canSetImage(this);
  }

  private boolean canNotifyStatusChanged() {
    return requestCoordinator == null || requestCoordinator.canNotifyStatusChanged(this);
  }

  private boolean isFirstReadyResource() {
    return requestCoordinator == null || !requestCoordinator.isAnyResourceSet();
  }

  private void notifyLoadSuccess() {
    if (requestCoordinator != null) {
      requestCoordinator.onRequestSuccess(this);
    }
  }

  /**
   * A callback method that should never be invoked directly.
   */
  @SuppressWarnings("unchecked")
  @Override
  public void onResourceReady(Resource<?> resource) {
    loadStatus = null;
    if (resource == null) {
      GlideException exception = new GlideException("Expected to receive a Resource<R> with an "
          + "object of " + transcodeClass + " inside, but instead got null.");
      onLoadFailed(exception);
      return;
    }

    Object received = resource.get();
    if (received == null || !transcodeClass.isAssignableFrom(received.getClass())) {
      releaseResource(resource);
      GlideException exception = new GlideException("Expected to receive an object of "
          + transcodeClass + " but instead" + " got "
          + (received != null ? received.getClass() : "") + "{" + received + "} inside" + " "
          + "Resource{" + resource + "}."
          + (received != null ? "" : " " + "To indicate failure return a null Resource "
          + "object, rather than a Resource object containing null data."));
      onLoadFailed(exception);
      return;
    }

    if (!canSetResource()) {
      releaseResource(resource);
      // We can't put the status to complete before asking canSetResource().
      status = Status.COMPLETE;
      return;
    }

    onResourceReady((Resource<R>) resource, (R) received);
  }

  /**
   * Internal {@link #onResourceReady(Resource)} where arguments are known to be safe.
   *
   * @param resource original {@link Resource}, never <code>null</code>
   * @param result   object returned by {@link Resource#get()}, checked for type and never
   *                 <code>null</code>
   */
  private void onResourceReady(Resource<R> resource, R result) {
    // We must call isFirstReadyResource before setting status.
    boolean isFirstResource = isFirstReadyResource();
    status = Status.COMPLETE;
    this.resource = resource;

    if (requestListener == null
        || !requestListener.onResourceReady(result, model, target, loadedFromMemoryCache,
        isFirstResource)) {
      Transition<? super R> animation =
          animationFactory.build(loadedFromMemoryCache, isFirstResource);
      target.onResourceReady(result, animation);
    }

    notifyLoadSuccess();

    if (Log.isLoggable(TAG, Log.VERBOSE)) {
      logV("Resource ready in " + LogTime.getElapsedMillis(startTime) + " size: " + (
          resource.getSize() * TO_MEGABYTE) + " fromCache: " + loadedFromMemoryCache);
    }
  }

  /**
   * A callback method that should never be invoked directly.
   */
  @Override
  public void onLoadFailed(GlideException e) {
    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, "Load failed for: " + model, e);
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        e.logRootCauses(TAG);
      }
    }

    loadStatus = null;
    status = Status.FAILED;
    //TODO: what if this is a thumbnail request?
    if (requestListener == null || !requestListener.onLoadFailed(model, target,
        isFirstReadyResource())) {
      setErrorPlaceholder();
    }
  }

  private void logV(String message) {
    Log.v(TAG, message + " this: " + tag);
  }
}
