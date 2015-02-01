package com.bumptech.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;

import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.resource.bitmap.BitmapAnimationOptions;
import com.bumptech.glide.load.resource.drawable.DrawableAnimationOptions;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.manager.ConnectivityMonitor;
import com.bumptech.glide.manager.ConnectivityMonitorFactory;
import com.bumptech.glide.manager.Lifecycle;
import com.bumptech.glide.manager.LifecycleListener;
import com.bumptech.glide.manager.RequestTracker;
import com.bumptech.glide.request.GlideContext;
import com.bumptech.glide.util.Util;

/**
 * A class for managing and starting requests for Glide. Can use activity, fragment and connectivity lifecycle events to
 * intelligently stop, start, and restart requests. Retrieve either by instantiating a new object, or to take advantage
 * built in Activity and Fragment lifecycle handling, use the static Glide.load methods with your Fragment or Activity.
 *
 * @see Glide#with(android.app.Activity)
 * @see Glide#with(android.support.v4.app.FragmentActivity)
 * @see Glide#with(android.app.Fragment)
 * @see Glide#with(android.support.v4.app.Fragment)
 * @see Glide#with(Context)
 */
public final class RequestManager implements LifecycleListener {
    private final GlideContext context;
    private final Lifecycle lifecycle;
    private final RequestTracker requestTracker;

    public RequestManager(Context context, Lifecycle lifecycle) {
        this(context, lifecycle, new RequestTracker(), new ConnectivityMonitorFactory());
    }

    RequestManager(Context context, final Lifecycle lifecycle, RequestTracker requestTracker,
            ConnectivityMonitorFactory factory) {
        this.context = Glide.get(context).getGlideContext();
        this.lifecycle = lifecycle;
        this.requestTracker = requestTracker;

        ConnectivityMonitor connectivityMonitor = factory.build(context,
                new RequestManagerConnectivityListener(requestTracker));

        // If we're the application level request manager, we may be created on a background thread. In that case we
        // cannot risk synchronously pausing or resuming requests, so we hack around the issue by delaying adding
        // ourselves as a lifecycle listener by posting to the main thread. This should be entirely safe.
        if (Util.isOnBackgroundThread()) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    lifecycle.addListener(RequestManager.this);
                }
            });
        } else {
            lifecycle.addListener(this);
        }
        lifecycle.addListener(connectivityMonitor);
    }

    /**
     * @see android.content.ComponentCallbacks2#onTrimMemory(int)
     */
    public void onTrimMemory(int level) {
        context.onTrimMemory(level);
    }

    /**
     * @see android.content.ComponentCallbacks2#onLowMemory()
     */
    public void onLowMemory() {
        context.onLowMemory();
    }

    /**
     * Returns true if loads for this {@link RequestManager} are currently paused.
     *
     * @see #pauseRequests()
     * @see #resumeRequests()
     */
    public boolean isPaused() {
        Util.assertMainThread();
        return requestTracker.isPaused();
    }

    /**
     * Cancels any in progress loads, but does not clear resources of completed loads.
     *
     * @see #isPaused()
     * @see #resumeRequests()
     */
    public void pauseRequests() {
        Util.assertMainThread();
        requestTracker.pauseRequests();
    }

    /**
     * Restarts any loads that have not yet completed.
     *
     * @see #isPaused()
     * @see #pauseRequests()
     */
    public void resumeRequests() {
        Util.assertMainThread();
        requestTracker.resumeRequests();
    }

    /**
     * Lifecycle callback that registers for connectivity events (if the android.permission.ACCESS_NETWORK_STATE
     * permission is present) and restarts failed or paused requests.
     */
    @Override
    public void onStart() {
        // onStart might not be called because this object may be created after the fragment/activity's onStart method.
        resumeRequests();
    }

    /**
     * Lifecycle callback that unregisters for connectivity events (if the android.permission.ACCESS_NETWORK_STATE
     * permission is present) and pauses in progress loads.
     */
    @Override
    public void onStop() {
        pauseRequests();
    }

    /**
     * Lifecycle callback that cancels all in progress requests and clears and recycles resources for all completed
     * requests.
     */
    @Override
    public void onDestroy() {
        requestTracker.clearRequests();
    }

    /**
     * Attempts to always load the resource as a {@link android.graphics.Bitmap}, even if it could actually be animated.
     *
     * @return A new request builder for loading a {@link android.graphics.Bitmap}
     */
    public TranscodeRequest<Bitmap> asBitmap() {
        return (TranscodeRequest<Bitmap>) as(Bitmap.class)
                .animate(new BitmapAnimationOptions());
    }

    /**
     * Attempts to always load the resource as a {@link com.bumptech.glide.load.resource.gif.GifDrawable}.
     * <p>
     *     If the underlying data is not a GIF, this will fail. As a result, this should only be used if the model
     *     represents an animated GIF and the caller wants to interact with the GIfDrawable directly. Normally using
     *     just {@link #asDrawable()} is sufficient because it will determine whether or not the given data
     *     represents an animated GIF and return the appropriate {@link Drawable}, animated or not, automatically.
     * </p>
     *
     * @return A new request builder for loading a {@link com.bumptech.glide.load.resource.gif.GifDrawable}.
     */
    public TranscodeRequest<GifDrawable> asGif() {
        return (TranscodeRequest<GifDrawable>) as(GifDrawable.class)
                .animate(new DrawableAnimationOptions());
    }

    /**
     * Attempts to always load the resource using any registered {@link ResourceDecoder}s that can decode any
     * subclass of {@link Drawable}. By default, may return either a {@link BitmapDrawable} or {@link GifDrawable},
     * but if additional decoders are registered for other {@link Drawable} subclasses, any of those subclasses may
     * also be returned.
     *
     * @return A new request builder for loading a {@link Drawable}.
     */
    public TranscodeRequest<Drawable> asDrawable() {
        return (TranscodeRequest<Drawable>) as(Drawable.class)
                .animate(new DrawableAnimationOptions());
    }

    /**
     * Attempts to load the resource using any registered {@link ResourceDecoder}s that can decode the given resource
     * class or any subclass of the given resource class.
     *
     * @param resourceClass The resource to decode.
     * @return A new request builder for loading the given resource class.
     */
    public <ResourceType> TranscodeRequest<ResourceType> as(Class<ResourceType> resourceClass) {
        return new TranscodeRequest<ResourceType>(context, resourceClass, requestTracker, lifecycle);
    }

    private static class RequestManagerConnectivityListener implements ConnectivityMonitor.ConnectivityListener {
        private final RequestTracker requestTracker;

        public RequestManagerConnectivityListener(RequestTracker requestTracker) {
            this.requestTracker = requestTracker;
        }

        @Override
        public void onConnectivityChanged(boolean isConnected) {
            if (isConnected) {
                requestTracker.restartRequests();
            }
        }
    }
}
