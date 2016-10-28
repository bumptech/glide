package com.bumptech.glide;

import static com.bumptech.glide.request.RequestOptions.decodeTypeOf;
import static com.bumptech.glide.request.RequestOptions.diskCacheStrategyOf;
import static com.bumptech.glide.request.RequestOptions.skipMemoryCacheOf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.manager.ConnectivityMonitor;
import com.bumptech.glide.manager.ConnectivityMonitorFactory;
import com.bumptech.glide.manager.Lifecycle;
import com.bumptech.glide.manager.LifecycleListener;
import com.bumptech.glide.manager.RequestManagerTreeNode;
import com.bumptech.glide.manager.RequestTracker;
import com.bumptech.glide.manager.TargetTracker;
import com.bumptech.glide.request.BaseRequestOptions;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.target.ViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.util.Synthetic;
import com.bumptech.glide.util.Util;
import java.io.File;

/**
 * A class for managing and starting requests for Glide. Can use activity, fragment and connectivity
 * lifecycle events to intelligently stop, start, and restart requests. Retrieve either by
 * instantiating a new object, or to take advantage built in Activity and Fragment lifecycle
 * handling, use the static Glide.load methods with your Fragment or Activity.
 *
 * @see Glide#with(android.app.Activity)
 * @see Glide#with(android.support.v4.app.FragmentActivity)
 * @see Glide#with(android.app.Fragment)
 * @see Glide#with(android.support.v4.app.Fragment)
 * @see Glide#with(Context)
 */
public class RequestManager implements LifecycleListener {
  private static final RequestOptions DECODE_TYPE_BITMAP = decodeTypeOf(Bitmap.class).lock();
  private static final RequestOptions DECODE_TYPE_GIF = decodeTypeOf(GifDrawable.class).lock();
  private static final RequestOptions DOWNLOAD_ONLY_OPTIONS =
      diskCacheStrategyOf(DiskCacheStrategy.DATA).priority(Priority.LOW)
          .skipMemoryCache(true);

  private final Glide glide;
  @Synthetic final Lifecycle lifecycle;
  private final RequestTracker requestTracker;
  private final RequestManagerTreeNode treeNode;
  private final TargetTracker targetTracker = new TargetTracker();
  private final Runnable addSelfToLifecycle = new Runnable() {
    @Override
    public void run() {
      lifecycle.addListener(RequestManager.this);
    }
  };
  private final Handler mainHandler = new Handler(Looper.getMainLooper());
  private final ConnectivityMonitor connectivityMonitor;

  @NonNull
  private BaseRequestOptions<?> defaultRequestOptions;
  @NonNull
  private BaseRequestOptions<?> requestOptions;

  public RequestManager(Glide glide, Lifecycle lifecycle, RequestManagerTreeNode treeNode) {
    this(glide, lifecycle, treeNode, new RequestTracker(), glide.getConnectivityMonitorFactory());
  }

  RequestManager(
      Glide glide,
      Lifecycle lifecycle,
      RequestManagerTreeNode treeNode,
      RequestTracker requestTracker,
      ConnectivityMonitorFactory factory) {
    this.glide = glide;
    this.lifecycle = lifecycle;
    this.treeNode = treeNode;
    this.requestTracker = requestTracker;

    final Context context = glide.getGlideContext().getBaseContext();


    connectivityMonitor =
        factory.build(context, new RequestManagerConnectivityListener(requestTracker));

    // If we're the application level request manager, we may be created on a background thread.
    // In that case we cannot risk synchronously pausing or resuming requests, so we hack around the
    // issue by delaying adding ourselves as a lifecycle listener by posting to the main thread.
    // This should be entirely safe.
    if (Util.isOnBackgroundThread()) {
      mainHandler.post(addSelfToLifecycle);
    } else {
      lifecycle.addListener(this);
    }
    lifecycle.addListener(connectivityMonitor);

    defaultRequestOptions = glide.getGlideContext().getDefaultRequestOptions();
    requestOptions = defaultRequestOptions;

    glide.registerRequestManager(this);
  }

  /**
   * Updates the default {@link RequestOptions} for all loads started with this request manager
   * with the given {@link RequestOptions}.
   *
   * <p>The {@link RequestOptions} provided here are applied on top of those provided via {@link
   * GlideBuilder#setDefaultRequestOptions(RequestOptions)}. If there are conflicts, the options
   * applied here will win. Note that this method does not mutate options provided to
   * {@link GlideBuilder#setDefaultRequestOptions(RequestOptions)}.
   *
   * <p>Multiple sets of options can be applied. If there are conflicts the last {@link
   * RequestOptions} applied will win.
   *
   * <p>The modified options will only be applied to loads started after this method is called.
   *
   * @see RequestBuilder#apply(BaseRequestOptions)
   *
   * @return This request manager.
   */
  public RequestManager applyDefaultRequestOptions(RequestOptions requestOptions) {
    BaseRequestOptions<?> toMutate = this.requestOptions == defaultRequestOptions
        ? this.requestOptions.clone() : this.defaultRequestOptions;
    this.requestOptions = toMutate.apply(requestOptions);
    return this;
  }

  /**
   * Replaces the default {@link RequestOptions} for all loads started with this request manager
   * with the given {@link RequestOptions}.
   *
   * <p>The {@link RequestOptions} provided here replace those that have been previously provided
   * via {@link GlideBuilder#setDefaultRequestOptions(RequestOptions)}, {@link
   * #setDefaultRequestOptions(RequestOptions)} and {@link
   * #applyDefaultRequestOptions(RequestOptions)}.
   *
   * <p>Subsequent calls to {@link #applyDefaultRequestOptions(RequestOptions)} will not mutate
   * the {@link RequestOptions} provided here. Instead the manager will create a clone of these
   * options and mutate the clone.
   *
   * @see #applyDefaultRequestOptions(RequestOptions)
   *
   * @return This request manager.
   */
  public RequestManager setDefaultRequestOptions(RequestOptions requestOptions) {
    this.defaultRequestOptions = requestOptions;
    this.requestOptions = requestOptions;
    return this;
  }

  /**
   * @see android.content.ComponentCallbacks2#onTrimMemory(int)
   */
  public void onTrimMemory(int level) {
    glide.getGlideContext().onTrimMemory(level);
  }

  /**
   * @see android.content.ComponentCallbacks2#onLowMemory()
   */
  public void onLowMemory() {
    glide.getGlideContext().onLowMemory();
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
   * Performs {@link #pauseRequests()} recursively for all managers that are contextually
   * descendant to this manager based on the Activity/Fragment hierarchy:
   *
   * <ul>
   *   <li>When pausing on an Activity all attached fragments will also get paused.
   *   <li>When pausing on an attached Fragment all descendant fragments will also get paused.
   *   <li>When pausing on a detached Fragment or the application context only the current
   *   RequestManager is paused.
   * </ul>
   *
   * <p>Note, on pre-Jelly Bean MR1 calling pause on a Fragment will not cause child fragments to
   * pause, in this case either call pause on the Activity or use a support Fragment.
   */
  public void pauseRequestsRecursive() {
    Util.assertMainThread();
    pauseRequests();
    for (RequestManager requestManager : treeNode.getDescendants()) {
      requestManager.pauseRequests();
    }
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
   * Performs {@link #resumeRequests()} recursively for all managers that are contextually
   * descendant to this manager based on the Activity/Fragment hierarchy. The hierarchical semantics
   * are identical as for {@link #pauseRequestsRecursive()}.
   */
  public void resumeRequestsRecursive() {
    Util.assertMainThread();
    resumeRequests();
    for (RequestManager requestManager : treeNode.getDescendants()) {
      requestManager.resumeRequests();
    }
  }

  /**
   * Lifecycle callback that registers for connectivity events (if the
   * android.permission.ACCESS_NETWORK_STATE permission is present) and restarts failed or paused
   * requests.
   */
  @Override
  public void onStart() {
    resumeRequests();
    targetTracker.onStart();
  }

  /**
   * Lifecycle callback that unregisters for connectivity events (if the
   * android.permission.ACCESS_NETWORK_STATE permission is present) and pauses in progress loads.
   */
  @Override
  public void onStop() {
    pauseRequests();
    targetTracker.onStop();
  }

  /**
   * Lifecycle callback that cancels all in progress requests and clears and recycles resources for
   * all completed requests.
   */
  @Override
  public void onDestroy() {
    targetTracker.onDestroy();
    for (Target<?> target : targetTracker.getAll()) {
      clear(target);
    }
    targetTracker.clear();
    requestTracker.clearRequests();
    lifecycle.removeListener(this);
    lifecycle.removeListener(connectivityMonitor);
    mainHandler.removeCallbacks(addSelfToLifecycle);
    glide.unregisterRequestManager(this);
  }

  /**
   * Attempts to always load the resource as a {@link android.graphics.Bitmap}, even if it could
   * actually be animated.
   *
   * @return A new request builder for loading a {@link android.graphics.Bitmap}
   */
  public RequestBuilder<Bitmap> asBitmap() {
    return as(Bitmap.class).transition(new GenericTransitionOptions<Bitmap>())
            .apply(DECODE_TYPE_BITMAP);
  }

  /**
   * Attempts to always load the resource as a
   * {@link com.bumptech.glide.load.resource.gif.GifDrawable}.
   *
   * <p> If the underlying data is not a GIF, this will fail. As a result, this should only be used
   * if the model represents an animated GIF and the caller wants to interact with the GifDrawable
   * directly. Normally using just {@link #asDrawable()} is sufficient because it will determine
   * whether or not the given data represents an animated GIF and return the appropriate {@link
   * Drawable}, animated or not, automatically. </p>
   *
   * @return A new request builder for loading a
   * {@link com.bumptech.glide.load.resource.gif.GifDrawable}.
   */
  public RequestBuilder<GifDrawable> asGif() {
    return as(GifDrawable.class).transition(new DrawableTransitionOptions()).apply(DECODE_TYPE_GIF);
  }

  /**
   * Attempts to always load the resource using any registered {@link
   * com.bumptech.glide.load.ResourceDecoder}s that can decode any subclass of {@link Drawable}.
   *
   * <p> By default, may return either a {@link android.graphics.drawable.BitmapDrawable} or {@link
   * GifDrawable}, but if additional decoders are registered for other {@link Drawable} subclasses,
   * any of those subclasses may also be returned. </p>
   *
   * @return A new request builder for loading a {@link Drawable}.
   */
  public RequestBuilder<Drawable> asDrawable() {
    return as(Drawable.class).transition(new DrawableTransitionOptions());
  }

  /**
   * A helper method equivalent to calling {@link #asDrawable()} and then {@link
   * RequestBuilder#load(Object)} with the given model.
   *
   * @return A new request builder for loading a {@link Drawable} using the given model.
   */
  public RequestBuilder<Drawable> load(@Nullable Object model) {
    return asDrawable().load(model);
  }

  /**
   * Attempts always load the resource into the cache and return the {@link File} containing the
   * cached source data.
   *
   * <p>This method is designed to work for remote data that is or will be cached using {@link
   * com.bumptech.glide.load.engine.DiskCacheStrategy#DATA}. As a result, specifying a
   * {@link com.bumptech.glide.load.engine.DiskCacheStrategy} on this request is generally not
   * recommended.
   *
   * @return A new request builder for downloading content to cache and returning the cache File.
   */
  public RequestBuilder<File> downloadOnly() {
    return as(File.class).apply(DOWNLOAD_ONLY_OPTIONS);
  }

  /**
   * A helper method equivalent to calling {@link #downloadOnly()} ()} and then {@link
   * RequestBuilder#load(Object)} with the given model.
   *
   * @return A new request builder for loading a {@link Drawable} using the given model.
   */
  public RequestBuilder<File> download(@Nullable Object model) {
    return downloadOnly().load(model);
  }

  /**
   * Attempts to always load a {@link File} containing the resource, either using a file path
   * obtained from the media store (for local images/videos), or using Glide's disk cache
   * (for remote images/videos).
   *
   * <p>For remote content, prefer {@link #downloadOnly()}.
   *
   * @return A new request builder for obtaining File paths to content.
   */
  public RequestBuilder<File> asFile() {
    return as(File.class).apply(skipMemoryCacheOf(true));
  }

  /**
   * Attempts to load the resource using any registered
   * {@link com.bumptech.glide.load.ResourceDecoder}s
   * that can decode the given resource class or any subclass of the given resource class.
   *
   * @param resourceClass The resource to decode.
   * @return A new request builder for loading the given resource class.
   */
  public <ResourceType> RequestBuilder<ResourceType> as(Class<ResourceType> resourceClass) {
    return new RequestBuilder<>(glide.getGlideContext(), this, resourceClass);
  }

  /**
   * Cancel any pending loads Glide may have for the view and free any resources that may have been
   * loaded for the view.
   *
   * <p> Note that this will only work if {@link View#setTag(Object)} is not called on this view
   * outside of Glide. </p>
   *
   * @param view The view to cancel loads and free resources for.
   * @throws IllegalArgumentException if an object other than Glide's metadata is put as the view's
   *                                  tag.
   * @see #clear(Target)
   */
  public void clear(View view) {
    clear(new ClearTarget(view));
  }

  /**
   * Cancel any pending loads Glide may have for the target and free any resources (such as
   * {@link Bitmap}s) that may have been loaded for the target so they may be reused.
   *
   * @param target The Target to cancel loads for.
   */
  public void clear(@Nullable final Target<?> target) {
    if (target == null) {
      return;
    }

    if (Util.isOnMainThread()) {
      untrackOrDelegate(target);
    } else {
      mainHandler.post(new Runnable() {
        @Override
        public void run() {
          clear(target);
        }
      });
    }
  }

  private void untrackOrDelegate(Target<?> target) {
    boolean isOwnedByUs = untrack(target);
    if (!isOwnedByUs) {
      glide.removeFromManagers(target);
    }
  }

  boolean untrack(Target<?> target) {
    Request request = target.getRequest();
    // If the Target doesn't have a request, it's already been cleared.
    if (request == null) {
      return true;
    }

    if (requestTracker.clearRemoveAndRecycle(request)) {
      targetTracker.untrack(target);
      target.setRequest(null);
      return true;
    } else {
      return false;
    }
  }

  void track(Target<?> target, Request request) {
    targetTracker.track(target);
    requestTracker.runRequest(request);
  }

  BaseRequestOptions<?> getDefaultRequestOptions() {
    return requestOptions;
  }

  @Override
  public String toString() {
    return super.toString() + "{tracker=" + requestTracker + ", treeNode=" + treeNode + "}";
  }

  private static class RequestManagerConnectivityListener implements ConnectivityMonitor
      .ConnectivityListener {
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

  private static class ClearTarget extends ViewTarget<View, Object> {

    public ClearTarget(View view) {
      super(view);
    }

    @Override
    public void onResourceReady(Object resource, Transition<? super Object> transition) {
      // Do nothing.
    }
  }
}
