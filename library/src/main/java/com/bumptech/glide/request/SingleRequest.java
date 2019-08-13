package com.bumptech.glide.request;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.graphics.drawable.Drawable;
import android.util.Log;
import androidx.annotation.DrawableRes;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pools;
import com.bumptech.glide.GlideContext;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.Engine;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.drawable.DrawableDecoderCompat;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.request.transition.TransitionFactory;
import com.bumptech.glide.util.LogTime;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Synthetic;
import com.bumptech.glide.util.Util;
import com.bumptech.glide.util.pool.FactoryPools;
import com.bumptech.glide.util.pool.StateVerifier;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * A {@link Request} that loads a {@link com.bumptech.glide.load.engine.Resource} into a given
 * {@link Target}.
 *
 * @param <R> The type of the resource that will be transcoded from the loaded resource.
 */
@SuppressWarnings("SynchronizeOnNonFinalField")
public final class SingleRequest<R>
    implements Request, SizeReadyCallback, ResourceCallback, FactoryPools.Poolable {
  /** Tag for logging internal events, not generally suitable for public use. */
  private static final String TAG = "Request";
  /** Tag for logging externally useful events (request completion, timing etc). */
  private static final String GLIDE_TAG = "Glide";

  private static final Pools.Pool<SingleRequest<?>> POOL =
      FactoryPools.threadSafe(
          150,
          new FactoryPools.Factory<SingleRequest<?>>() {
            @Override
            public SingleRequest<?> create() {
              return new SingleRequest<Object>();
            }
          });

  private static final boolean IS_VERBOSE_LOGGABLE = Log.isLoggable(TAG, Log.VERBOSE);

  private enum Status {
    /** Created but not yet running. */
    PENDING,
    /** In the process of fetching media. */
    RUNNING,
    /** Waiting for a callback given to the Target to be called to determine target dimensions. */
    WAITING_FOR_SIZE,
    /** Finished loading media successfully. */
    COMPLETE,
    /** Failed to load media, may be restarted. */
    FAILED,
    /** Cleared by the user with a placeholder set, may be restarted. */
    CLEARED,
  }

  @Nullable
  private final String tag = IS_VERBOSE_LOGGABLE ? String.valueOf(super.hashCode()) : null;

  private final StateVerifier stateVerifier = StateVerifier.newInstance();

  /* Variables mutated only when a request is initialized or returned to the object pool. */
  private volatile Object requestLock;

  @GuardedBy("requestLock")
  @Nullable
  private RequestListener<R> targetListener;

  @GuardedBy("requestLock")
  private RequestCoordinator requestCoordinator;

  @GuardedBy("requestLock")
  private Context context;

  @GuardedBy("requestLock")
  private GlideContext glideContext;

  @GuardedBy("requestLock")
  @Nullable
  private Object model;

  @GuardedBy("requestLock")
  private Class<R> transcodeClass;

  @GuardedBy("requestLock")
  private BaseRequestOptions<?> requestOptions;

  @GuardedBy("requestLock")
  private int overrideWidth;

  @GuardedBy("requestLock")
  private int overrideHeight;

  @GuardedBy("requestLock")
  private Priority priority;

  @GuardedBy("requestLock")
  private Target<R> target;

  @GuardedBy("requestLock")
  @Nullable
  private List<RequestListener<R>> requestListeners;

  @GuardedBy("requestLock")
  private TransitionFactory<? super R> animationFactory;

  @GuardedBy("requestLock")
  private Executor callbackExecutor;

  @GuardedBy("requestLock")
  private Resource<R> resource;

  @GuardedBy("requestLock")
  private Engine.LoadStatus loadStatus;

  @GuardedBy("requestLock")
  private long startTime;

  // Volatile because it's accessed outside of a lock and nullable, even though in practice it will
  // always be non-null unless the request is in the object pool.
  private volatile Engine engine;

  /* Variables mutated during a request. */
  @GuardedBy("requestLock")
  private Status status;

  @GuardedBy("requestLock")
  private Drawable errorDrawable;

  @GuardedBy("requestLock")
  private Drawable placeholderDrawable;

  @GuardedBy("requestLock")
  private Drawable fallbackDrawable;

  @GuardedBy("requestLock")
  private int width;

  @GuardedBy("requestLock")
  private int height;

  @GuardedBy("requestLock")
  private boolean isCallingCallbacks;

  @Nullable private RuntimeException requestOrigin;

  public static <R> SingleRequest<R> obtain(
      Context context,
      GlideContext glideContext,
      @Nullable Object requestLock,
      Object model,
      Class<R> transcodeClass,
      BaseRequestOptions<?> requestOptions,
      int overrideWidth,
      int overrideHeight,
      Priority priority,
      Target<R> target,
      RequestListener<R> targetListener,
      @Nullable List<RequestListener<R>> requestListeners,
      RequestCoordinator requestCoordinator,
      Engine engine,
      TransitionFactory<? super R> animationFactory,
      Executor callbackExecutor) {
    @SuppressWarnings("unchecked")
    SingleRequest<R> request = (SingleRequest<R>) POOL.acquire();
    if (request == null) {
      request = new SingleRequest<>();
    }
    if (requestLock == null) {
      requestLock = request;
    }
    request.init(
        context,
        glideContext,
        requestLock,
        model,
        transcodeClass,
        requestOptions,
        overrideWidth,
        overrideHeight,
        priority,
        target,
        targetListener,
        requestListeners,
        requestCoordinator,
        engine,
        animationFactory,
        callbackExecutor);
    return request;
  }

  @SuppressWarnings("WeakerAccess")
  @Synthetic
  SingleRequest() {
    // just create, instances are reused with recycle/init
  }

  // We are in fact locking on the same lock that will be used for all subsequent method calls.
  @SuppressWarnings("GuardedBy")
  private void init(
      Context context,
      GlideContext glideContext,
      @NonNull Object requestLock,
      Object model,
      Class<R> transcodeClass,
      BaseRequestOptions<?> requestOptions,
      int overrideWidth,
      int overrideHeight,
      Priority priority,
      Target<R> target,
      RequestListener<R> targetListener,
      @Nullable List<RequestListener<R>> requestListeners,
      RequestCoordinator requestCoordinator,
      Engine engine,
      TransitionFactory<? super R> animationFactory,
      Executor callbackExecutor) {
    this.requestLock = Preconditions.checkNotNull(requestLock);
    synchronized (this.requestLock) {
      this.context = context;
      this.glideContext = glideContext;
      this.model = model;
      this.transcodeClass = transcodeClass;
      this.requestOptions = requestOptions;
      this.overrideWidth = overrideWidth;
      this.overrideHeight = overrideHeight;
      this.priority = priority;
      this.target = target;
      this.targetListener = targetListener;
      this.requestListeners = requestListeners;
      this.requestCoordinator = requestCoordinator;
      this.engine = engine;
      this.animationFactory = animationFactory;
      this.callbackExecutor = callbackExecutor;
      status = Status.PENDING;

      if (requestOrigin == null && glideContext.isLoggingRequestOriginsEnabled()) {
        requestOrigin = new RuntimeException("Glide request origin trace");
      }
    }
  }

  @NonNull
  @Override
  public StateVerifier getVerifier() {
    return stateVerifier;
  }

  @Override
  public void recycle() {
    synchronized (requestLock) {
      assertNotCallingCallbacks();
      context = null;
      glideContext = null;
      model = null;
      transcodeClass = null;
      requestOptions = null;
      overrideWidth = -1;
      overrideHeight = -1;
      target = null;
      requestListeners = null;
      targetListener = null;
      requestCoordinator = null;
      animationFactory = null;
      loadStatus = null;
      errorDrawable = null;
      placeholderDrawable = null;
      fallbackDrawable = null;
      width = -1;
      height = -1;
      requestOrigin = null;
      POOL.release(this);
    }
    requestLock = null;
  }

  @Override
  public void begin() {
    synchronized (requestLock) {
      assertNotCallingCallbacks();
      stateVerifier.throwIfRecycled();
      startTime = LogTime.getLogTime();
      if (model == null) {
        if (Util.isValidDimensions(overrideWidth, overrideHeight)) {
          width = overrideWidth;
          height = overrideHeight;
        }
        // Only log at more verbose log levels if the user has set a fallback drawable, because
        // fallback Drawables indicate the user expects null models occasionally.
        int logLevel = getFallbackDrawable() == null ? Log.WARN : Log.DEBUG;
        onLoadFailed(new GlideException("Received null model"), logLevel);
        return;
      }

      if (status == Status.RUNNING) {
        throw new IllegalArgumentException("Cannot restart a running request");
      }

      // If we're restarted after we're complete (usually via something like a notifyDataSetChanged
      // that starts an identical request into the same Target or View), we can simply use the
      // resource and size we retrieved the last time around and skip obtaining a new size, starting
      // a new load etc. This does mean that users who want to restart a load because they expect
      // that the view size has changed will need to explicitly clear the View or Target before
      // starting the new load.
      if (status == Status.COMPLETE) {
        onResourceReady(resource, DataSource.MEMORY_CACHE);
        return;
      }

      // Restarts for requests that are neither complete nor running can be treated as new requests
      // and can run again from the beginning.

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
      if (IS_VERBOSE_LOGGABLE) {
        logV("finished run method in " + LogTime.getElapsedMillis(startTime));
      }
    }
  }

  /**
   * Cancels the current load but does not release any resources held by the request and continues
   * to display the loaded resource if the load completed before the call to cancel.
   *
   * <p>Cancelled requests can be restarted with a subsequent call to {@link #begin()}.
   *
   * @see #clear()
   */
  @GuardedBy("requestLock")
  private void cancel() {
    assertNotCallingCallbacks();
    stateVerifier.throwIfRecycled();
    target.removeCallback(this);
    if (loadStatus != null) {
      loadStatus.cancel();
      loadStatus = null;
    }
  }

  // Avoids difficult to understand errors like #2413.
  @GuardedBy("requestLock")
  private void assertNotCallingCallbacks() {
    if (isCallingCallbacks) {
      throw new IllegalStateException(
          "You can't start or clear loads in RequestListener or"
              + " Target callbacks. If you're trying to start a fallback request when a load fails,"
              + " use RequestBuilder#error(RequestBuilder). Otherwise consider posting your into()"
              + " or clear() calls to the main thread using a Handler instead.");
    }
  }

  /**
   * Cancels the current load if it is in progress, clears any resources held onto by the request
   * and replaces the loaded resource if the load completed with the placeholder.
   *
   * <p>Cleared requests can be restarted with a subsequent call to {@link #begin()}
   *
   * @see #cancel()
   */
  @Override
  public void clear() {
    Resource<R> toRelease = null;
    synchronized (requestLock) {
      assertNotCallingCallbacks();
      stateVerifier.throwIfRecycled();
      if (status == Status.CLEARED) {
        return;
      }
      cancel();
      // Resource must be released before canNotifyStatusChanged is called.
      if (resource != null) {
        toRelease = resource;
        resource = null;
      }
      if (canNotifyCleared()) {
        target.onLoadCleared(getPlaceholderDrawable());
      }

      status = Status.CLEARED;
    }

    if (toRelease != null) {
      engine.release(toRelease);
    }
  }

  @Override
  public void pause() {
    synchronized (requestLock) {
      if (isRunning()) {
        clear();
      }
    }
  }

  @Override
  public boolean isRunning() {
    synchronized (requestLock) {
      return status == Status.RUNNING || status == Status.WAITING_FOR_SIZE;
    }
  }

  @Override
  public boolean isComplete() {
    synchronized (requestLock) {
      return status == Status.COMPLETE;
    }
  }

  @Override
  public boolean isCleared() {
    synchronized (requestLock) {
      return status == Status.CLEARED;
    }
  }

  @GuardedBy("requestLock")
  private Drawable getErrorDrawable() {
    if (errorDrawable == null) {
      errorDrawable = requestOptions.getErrorPlaceholder();
      if (errorDrawable == null && requestOptions.getErrorId() > 0) {
        errorDrawable = loadDrawable(requestOptions.getErrorId());
      }
    }
    return errorDrawable;
  }

  @GuardedBy("requestLock")
  private Drawable getPlaceholderDrawable() {
    if (placeholderDrawable == null) {
      placeholderDrawable = requestOptions.getPlaceholderDrawable();
      if (placeholderDrawable == null && requestOptions.getPlaceholderId() > 0) {
        placeholderDrawable = loadDrawable(requestOptions.getPlaceholderId());
      }
    }
    return placeholderDrawable;
  }

  @GuardedBy("requestLock")
  private Drawable getFallbackDrawable() {
    if (fallbackDrawable == null) {
      fallbackDrawable = requestOptions.getFallbackDrawable();
      if (fallbackDrawable == null && requestOptions.getFallbackId() > 0) {
        fallbackDrawable = loadDrawable(requestOptions.getFallbackId());
      }
    }
    return fallbackDrawable;
  }

  @GuardedBy("requestLock")
  private Drawable loadDrawable(@DrawableRes int resourceId) {
    Theme theme =
        requestOptions.getTheme() != null ? requestOptions.getTheme() : context.getTheme();
    return DrawableDecoderCompat.getDrawable(glideContext, resourceId, theme);
  }

  @GuardedBy("requestLock")
  private void setErrorPlaceholder() {
    if (!canNotifyStatusChanged()) {
      return;
    }

    Drawable error = null;
    if (model == null) {
      error = getFallbackDrawable();
    }
    // Either the model isn't null, or there was no fallback drawable set.
    if (error == null) {
      error = getErrorDrawable();
    }
    // The model isn't null, no fallback drawable was set or no error drawable was set.
    if (error == null) {
      error = getPlaceholderDrawable();
    }
    target.onLoadFailed(error);
  }

  /** A callback method that should never be invoked directly. */
  @Override
  public void onSizeReady(int width, int height) {
    stateVerifier.throwIfRecycled();
    synchronized (requestLock) {
      if (IS_VERBOSE_LOGGABLE) {
        logV("Got onSizeReady in " + LogTime.getElapsedMillis(startTime));
      }
      if (status != Status.WAITING_FOR_SIZE) {
        return;
      }
      status = Status.RUNNING;

      float sizeMultiplier = requestOptions.getSizeMultiplier();
      this.width = maybeApplySizeMultiplier(width, sizeMultiplier);
      this.height = maybeApplySizeMultiplier(height, sizeMultiplier);

      if (IS_VERBOSE_LOGGABLE) {
        logV("finished setup for calling load in " + LogTime.getElapsedMillis(startTime));
      }
      loadStatus =
          engine.load(
              glideContext,
              model,
              requestOptions.getSignature(),
              this.width,
              this.height,
              requestOptions.getResourceClass(),
              transcodeClass,
              priority,
              requestOptions.getDiskCacheStrategy(),
              requestOptions.getTransformations(),
              requestOptions.isTransformationRequired(),
              requestOptions.isScaleOnlyOrNoTransform(),
              requestOptions.getOptions(),
              requestOptions.isMemoryCacheable(),
              requestOptions.getUseUnlimitedSourceGeneratorsPool(),
              requestOptions.getUseAnimationPool(),
              requestOptions.getOnlyRetrieveFromCache(),
              this,
              callbackExecutor);

      // This is a hack that's only useful for testing right now where loads complete synchronously
      // even though under any executor running on any thread but the main thread, the load would
      // have completed asynchronously.
      if (status != Status.RUNNING) {
        loadStatus = null;
      }
      if (IS_VERBOSE_LOGGABLE) {
        logV("finished onSizeReady in " + LogTime.getElapsedMillis(startTime));
      }
    }
  }

  private static int maybeApplySizeMultiplier(int size, float sizeMultiplier) {
    return size == Target.SIZE_ORIGINAL ? size : Math.round(sizeMultiplier * size);
  }

  @GuardedBy("requestLock")
  private boolean canSetResource() {
    return requestCoordinator == null || requestCoordinator.canSetImage(this);
  }

  @GuardedBy("requestLock")
  private boolean canNotifyCleared() {
    return requestCoordinator == null || requestCoordinator.canNotifyCleared(this);
  }

  @GuardedBy("requestLock")
  private boolean canNotifyStatusChanged() {
    return requestCoordinator == null || requestCoordinator.canNotifyStatusChanged(this);
  }

  @GuardedBy("requestLock")
  private boolean isFirstReadyResource() {
    return requestCoordinator == null || !requestCoordinator.isAnyResourceSet();
  }

  @GuardedBy("requestLock")
  private void notifyLoadSuccess() {
    if (requestCoordinator != null) {
      requestCoordinator.onRequestSuccess(this);
    }
  }

  @GuardedBy("requestLock")
  private void notifyLoadFailed() {
    if (requestCoordinator != null) {
      requestCoordinator.onRequestFailed(this);
    }
  }

  /** A callback method that should never be invoked directly. */
  @SuppressWarnings("unchecked")
  @Override
  public void onResourceReady(Resource<?> resource, DataSource dataSource) {
    stateVerifier.throwIfRecycled();
    Resource<?> toRelease = null;
    try {
      synchronized (requestLock) {
        loadStatus = null;
        if (resource == null) {
          GlideException exception =
              new GlideException(
                  "Expected to receive a Resource<R> with an "
                      + "object of "
                      + transcodeClass
                      + " inside, but instead got null.");
          onLoadFailed(exception);
          return;
        }

        Object received = resource.get();
        if (received == null || !transcodeClass.isAssignableFrom(received.getClass())) {
          toRelease = resource;
          this.resource = null;
          GlideException exception =
              new GlideException(
                  "Expected to receive an object of "
                      + transcodeClass
                      + " but instead"
                      + " got "
                      + (received != null ? received.getClass() : "")
                      + "{"
                      + received
                      + "} inside"
                      + " "
                      + "Resource{"
                      + resource
                      + "}."
                      + (received != null
                          ? ""
                          : " "
                              + "To indicate failure return a null Resource "
                              + "object, rather than a Resource object containing null data."));
          onLoadFailed(exception);
          return;
        }

        if (!canSetResource()) {
          toRelease = resource;
          this.resource = null;
          // We can't put the status to complete before asking canSetResource().
          status = Status.COMPLETE;
          return;
        }

        onResourceReady((Resource<R>) resource, (R) received, dataSource);
      }
    } finally {
      if (toRelease != null) {
        engine.release(toRelease);
      }
    }
  }

  /**
   * Internal {@link #onResourceReady(Resource, DataSource)} where arguments are known to be safe.
   *
   * @param resource original {@link Resource}, never <code>null</code>
   * @param result object returned by {@link Resource#get()}, checked for type and never <code>null
   *     </code>
   */
  @GuardedBy("requestLock")
  private void onResourceReady(Resource<R> resource, R result, DataSource dataSource) {
    // We must call isFirstReadyResource before setting status.
    boolean isFirstResource = isFirstReadyResource();
    status = Status.COMPLETE;
    this.resource = resource;

    if (glideContext.getLogLevel() <= Log.DEBUG) {
      Log.d(
          GLIDE_TAG,
          "Finished loading "
              + result.getClass().getSimpleName()
              + " from "
              + dataSource
              + " for "
              + model
              + " with size ["
              + width
              + "x"
              + height
              + "] in "
              + LogTime.getElapsedMillis(startTime)
              + " ms");
    }

    isCallingCallbacks = true;
    try {
      boolean anyListenerHandledUpdatingTarget = false;
      if (requestListeners != null) {
        for (RequestListener<R> listener : requestListeners) {
          anyListenerHandledUpdatingTarget |=
              listener.onResourceReady(result, model, target, dataSource, isFirstResource);
        }
      }
      anyListenerHandledUpdatingTarget |=
          targetListener != null
              && targetListener.onResourceReady(result, model, target, dataSource, isFirstResource);

      if (!anyListenerHandledUpdatingTarget) {
        Transition<? super R> animation = animationFactory.build(dataSource, isFirstResource);
        target.onResourceReady(result, animation);
      }
    } finally {
      isCallingCallbacks = false;
    }

    notifyLoadSuccess();
  }

  /** A callback method that should never be invoked directly. */
  @Override
  public void onLoadFailed(GlideException e) {
    onLoadFailed(e, Log.WARN);
  }

  @Override
  public Object getLock() {
    stateVerifier.throwIfRecycled();
    return requestLock;
  }

  private void onLoadFailed(GlideException e, int maxLogLevel) {
    stateVerifier.throwIfRecycled();
    synchronized (requestLock) {
      e.setOrigin(requestOrigin);
      int logLevel = glideContext.getLogLevel();
      if (logLevel <= maxLogLevel) {
        Log.w(
            GLIDE_TAG, "Load failed for " + model + " with size [" + width + "x" + height + "]", e);
        if (logLevel <= Log.INFO) {
          e.logRootCauses(GLIDE_TAG);
        }
      }

      loadStatus = null;
      status = Status.FAILED;

      isCallingCallbacks = true;
      try {
        // TODO: what if this is a thumbnail request?
        boolean anyListenerHandledUpdatingTarget = false;
        if (requestListeners != null) {
          for (RequestListener<R> listener : requestListeners) {
            anyListenerHandledUpdatingTarget |=
                listener.onLoadFailed(e, model, target, isFirstReadyResource());
          }
        }
        anyListenerHandledUpdatingTarget |=
            targetListener != null
                && targetListener.onLoadFailed(e, model, target, isFirstReadyResource());

        if (!anyListenerHandledUpdatingTarget) {
          setErrorPlaceholder();
        }
      } finally {
        isCallingCallbacks = false;
      }

      notifyLoadFailed();
    }
  }

  @Override
  public boolean isEquivalentTo(Request o) {
    if (!(o instanceof SingleRequest)) {
      return false;
    }

    int localOverrideWidth;
    int localOverrideHeight;
    Object localModel;
    Class<?> localTransocdeClass;
    BaseRequestOptions<?> localRequestOptions;
    Priority localPriority;
    int localListenerCount;
    synchronized (requestLock) {
      localOverrideWidth = overrideWidth;
      localOverrideHeight = overrideHeight;
      localModel = model;
      localTransocdeClass = transcodeClass;
      localRequestOptions = requestOptions;
      localPriority = priority;
      localListenerCount = requestListeners != null ? requestListeners.size() : 0;
    }

    SingleRequest<?> other = (SingleRequest<?>) o;
    int otherLocalOverrideWidth;
    int otherLocalOverrideHeight;
    Object otherLocalModel;
    Class<?> otherLocalTransocdeClass;
    BaseRequestOptions<?> otherLocalRequestOptions;
    Priority otherLocalPriority;
    int otherLocalListenerCount;
    synchronized (other.requestLock) {
      otherLocalOverrideWidth = other.overrideWidth;
      otherLocalOverrideHeight = other.overrideHeight;
      otherLocalModel = other.model;
      otherLocalTransocdeClass = other.transcodeClass;
      otherLocalRequestOptions = other.requestOptions;
      otherLocalPriority = other.priority;
      otherLocalListenerCount = other.requestListeners != null ? other.requestListeners.size() : 0;
    }

    // If there's ever a case where synchronization matters for these values, something else has
    // gone wrong. It indicates that we'er comparing at least one recycled object, which has to be
    // protected against via other means. None of these values changes aside from object re-use.
    return localOverrideWidth == otherLocalOverrideWidth
        && localOverrideHeight == otherLocalOverrideHeight
        && Util.bothModelsNullEquivalentOrEquals(localModel, otherLocalModel)
        && localTransocdeClass.equals(otherLocalTransocdeClass)
        && localRequestOptions.equals(otherLocalRequestOptions)
        && localPriority == otherLocalPriority
        // We do not want to require that RequestListeners implement equals/hashcode, so we
        // don't compare them using equals(). We can however, at least assert that the same
        // amount of request listeners are present in both requests.
        && localListenerCount == otherLocalListenerCount;
  }

  private void logV(String message) {
    Log.v(TAG, message + " this: " + tag);
  }
}
