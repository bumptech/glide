package com.bumptech.glide.request;

import android.graphics.drawable.Drawable;
import android.util.Log;

import com.bumptech.glide.load.engine.Engine;
import com.bumptech.glide.load.engine.RequestContext;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.request.transition.TransitionFactory;
import com.bumptech.glide.util.LogTime;
import com.bumptech.glide.util.Util;

import java.util.Queue;

/**
 * A {@link Request} that loads a {@link com.bumptech.glide.load.engine.Resource} into a given {@link Target}.
 *
 * @param <R> The type of the resource that will be transcoded from the loaded resource.
 */
public final class SingleRequest<R> implements Request,
        SizeReadyCallback,
        ResourceCallback {
    private static final String TAG = "Request";
    private static final Queue<SingleRequest<?>> REQUEST_POOL = Util.createQueue(0);
    private static final double TO_MEGABYTE = 1d / (1024d * 1024d);

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
        /** Cancelled by the user, may not be restarted. */
        CANCELLED,
        /** Cleared by the user with a placeholder set, may not be restarted. */
        CLEARED,
        /** Temporarily paused by the system, may be restarted. */
        PAUSED,
    }

    private final String tag = String.valueOf(hashCode());

    private RequestContext<R> requestContext;
    private Object model;
    private RequestOptions requestOptions;
    private RequestCoordinator requestCoordinator;
    private Class<R> transcodeClass;
    private Target<R> target;
    private RequestListener<R> requestListener;
    private Engine engine;
    private TransitionFactory<? super R> animationFactory;
    private Drawable placeholderDrawable;
    private Drawable errorDrawable;
    private boolean loadedFromMemoryCache;
    private Resource<R> resource;
    private Engine.LoadStatus loadStatus;
    private long startTime;
    private Status status;

    public static <R> SingleRequest<R> obtain(
            RequestContext<R> requestContext,
            Object model,
            Class<R> transcodeClass,
            RequestOptions requestOptions,
            Target<R> target,
            RequestListener<R> requestListener,
            RequestCoordinator requestCoordinator,
            Engine engine,
            TransitionFactory<? super R> animationFactory) {
        @SuppressWarnings("unchecked") SingleRequest<R> request = (SingleRequest<R>) REQUEST_POOL.poll();
        if (request == null) {
            request = new SingleRequest<>();
        }
        request.init(
                requestContext,
                model,
                transcodeClass,
                requestOptions,
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
            RequestContext<R> requestContext,
            Object model,
            Class<R> transcodeClass,
            RequestOptions requestOptions,
            Target<R> target,
            RequestListener<R> requestListener,
            RequestCoordinator requestCoordinator,
            Engine engine,
            TransitionFactory<? super R> animationFactory) {
        this.requestContext = requestContext;
        this.model = model;
        this.requestOptions = requestOptions;
        this.target = target;
        this.requestListener = requestListener;
        this.requestCoordinator = requestCoordinator;
        this.engine = engine;
        this.transcodeClass = transcodeClass;
        this.animationFactory = animationFactory;
        status = Status.PENDING;
    }

    @Override
    public void recycle() {
        requestContext = null;
        requestOptions = null;
        target = null;
        placeholderDrawable = null;
        errorDrawable = null;
        requestListener = null;
        requestCoordinator = null;
        animationFactory = null;
        loadedFromMemoryCache = false;
        loadStatus = null;
        REQUEST_POOL.offer(this);
    }

    @Override
    public void begin() {
        startTime = LogTime.getLogTime();
        if (model == null) {
            onException(null);
            return;
        }

        status = Status.WAITING_FOR_SIZE;
        int overrideWidth = requestOptions.getOverrideWidth();
        int overrideHeight = requestOptions.getOverrideHeight();
        if (overrideWidth > 0 && overrideHeight > 0) {
            onSizeReady(overrideWidth, overrideHeight);
        } else {
            target.getSize(this);
        }

        if (!isComplete() && !isFailed() && canNotifyStatusChanged()) {
            target.onLoadStarted(getPlaceholderDrawable());
        }
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            logV("finished run method in " + LogTime.getElapsedMillis(startTime));
        }
    }

    /**
     * Cancels the current load but does not release any resources held by the request and continues to display
     * the loaded resource if the load completed before the call to cancel.
     *
     * <p>
     *     Cancelled requests can be restarted with a subsequent call to {@link #begin()}.
     * </p>
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
     * Cancels the current load if it is in progress, clears any resources held onto by the request and replaces
     * the loaded resource if the load completed with the placeholder.
     *
     * <p>
     *     Cleared requests can be restarted with a subsequent call to {@link #begin()}
     * </p>
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

    private void setErrorPlaceholder(Exception e) {
        if (!canNotifyStatusChanged()) {
            return;
        }

        Drawable error = getErrorDrawable();
        if (error == null) {
            error = getPlaceholderDrawable();
        }
        target.onLoadFailed(e, error);
    }

    private Drawable getErrorDrawable() {
        if (errorDrawable == null) {
            errorDrawable = requestOptions.getErrorPlaceholder();
            if (errorDrawable == null && requestOptions.getErrorId() > 0) {
                errorDrawable = requestContext.getResources().getDrawable(requestOptions.getErrorId());
            }
        }
        return errorDrawable;
    }

    private Drawable getPlaceholderDrawable() {
        if (placeholderDrawable == null) {
            placeholderDrawable = requestOptions.getPlaceholderDrawable();
            if (placeholderDrawable == null && requestOptions.getPlaceholderId() > 0) {
                placeholderDrawable = requestContext.getResources().getDrawable(requestOptions.getPlaceholderId());
            }
        }
        return placeholderDrawable;
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
        loadStatus = engine.load(requestContext, width, height, this);
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
        if (resource == null) {
            onException(new Exception("Expected to receive a Resource<R> with an object of " + transcodeClass
                    + " inside, but instead got null."));
            return;
        }

        Object received = resource.get();
        if (received == null || !transcodeClass.isAssignableFrom(received.getClass())) {
            releaseResource(resource);
            onException(new Exception("Expected to receive an object of " + transcodeClass
                    + " but instead got " + (received != null ? received.getClass() : "") + "{" + received + "}"
                    + " inside Resource{" + resource + "}."
                    + (received != null ? "" : " "
                        + "To indicate failure return a null Resource object, "
                        + "rather than a Resource object containing null data.")
            ));
            return;
        }

        if (!canSetResource()) {
            releaseResource(resource);
            // We can't set the status to complete before asking canSetResource().
            status = Status.COMPLETE;
            return;
        }

        onResourceReady((Resource<R>) resource, (R) received);
    }

    /**
     * Internal {@link #onResourceReady(Resource)} where arguments are known to be safe.
     *
     * @param resource original {@link Resource}, never <code>null</code>
     * @param result object returned by {@link Resource#get()}, checked for type and never <code>null</code>
     */
    private void onResourceReady(Resource<R> resource, R result) {
        if (requestListener == null || !requestListener.onResourceReady(result, model, target, loadedFromMemoryCache,
                isFirstReadyResource())) {
            Transition<? super R> animation = animationFactory.build(loadedFromMemoryCache, isFirstReadyResource());
            target.onResourceReady(result, animation);
        }

        status = Status.COMPLETE;
        this.resource = resource;
        notifyLoadSuccess();

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            logV("Resource ready in " + LogTime.getElapsedMillis(startTime) + " size: "
                    + (resource.getSize() * TO_MEGABYTE) + " fromCache: " + loadedFromMemoryCache);
        }
    }

    /**
     * A callback method that should never be invoked directly.
     */
    @Override
    public void onException(Exception e) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "load failed", e);
        }

        status = Status.FAILED;
        //TODO: what if this is a thumbnail request?
        if (requestListener == null || !requestListener.onException(e, model, target, isFirstReadyResource())) {
            setErrorPlaceholder(e);
        }
    }

    private void logV(String message) {
        Log.v(TAG, message + " this: " + tag);
    }
}
