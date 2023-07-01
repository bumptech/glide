package com.bumptech.glide;

import static com.bumptech.glide.request.RequestOptions.decodeTypeOf;
import static com.bumptech.glide.request.RequestOptions.diskCacheStrategyOf;
import static com.bumptech.glide.request.RequestOptions.skipMemoryCacheOf;

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import androidx.annotation.CheckResult;
import androidx.annotation.DrawableRes;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
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
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomViewTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.util.Synthetic;
import com.bumptech.glide.util.Util;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A class for managing and starting requests for Glide. Can use activity, fragment and connectivity
 * lifecycle events to intelligently stop, start, and restart requests. Retrieve either by
 * instantiating a new object, or to take advantage built in Activity and Fragment lifecycle
 * handling, use the static Glide.load methods with your Fragment or Activity.
 *
 * @see Glide#with(android.app.Activity)
 * @see Glide#with(androidx.fragment.app.FragmentActivity)
 * @see Glide#with(android.app.Fragment)
 * @see Glide#with(androidx.fragment.app.Fragment)
 * @see Glide#with(Context)
 */
public class RequestManager
    implements ComponentCallbacks2, LifecycleListener, ModelTypes<RequestBuilder<Drawable>> {
  private static final RequestOptions DECODE_TYPE_BITMAP = decodeTypeOf(Bitmap.class).lock();
  private static final RequestOptions DECODE_TYPE_GIF = decodeTypeOf(GifDrawable.class).lock();
  private static final RequestOptions DOWNLOAD_ONLY_OPTIONS =
      diskCacheStrategyOf(DiskCacheStrategy.DATA).priority(Priority.LOW).skipMemoryCache(true);

  protected final Glide glide;
  protected final Context context;

  @SuppressWarnings("WeakerAccess")
  @Synthetic
  final Lifecycle lifecycle;

  @GuardedBy("this")
  private final RequestTracker requestTracker;

  @GuardedBy("this")
  private final RequestManagerTreeNode treeNode;

  @GuardedBy("this")
  private final TargetTracker targetTracker = new TargetTracker();

  private final Runnable addSelfToLifecycle =
      new Runnable() {
        @Override
        public void run() {
          lifecycle.addListener(RequestManager.this);
        }
      };
  private final ConnectivityMonitor connectivityMonitor;
  // Adding default listeners should be much less common than starting new requests. We want
  // some way of making sure that requests don't mutate our listeners without creating a new copy of
  // the list each time a request is started.
  private final CopyOnWriteArrayList<RequestListener<Object>> defaultRequestListeners;

  @GuardedBy("this")
  private RequestOptions requestOptions;

  private boolean pauseAllRequestsOnTrimMemoryModerate;

  private boolean clearOnStop;

  public RequestManager(
      @NonNull Glide glide,
      @NonNull Lifecycle lifecycle,
      @NonNull RequestManagerTreeNode treeNode,
      @NonNull Context context) {
    this(
        glide,
        lifecycle,
        treeNode,
        new RequestTracker(),
        glide.getConnectivityMonitorFactory(),
        context);
  }

  // Our usage is safe here.
  @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
  RequestManager(
      Glide glide,
      Lifecycle lifecycle,
      RequestManagerTreeNode treeNode,
      RequestTracker requestTracker,
      ConnectivityMonitorFactory factory,
      Context context) {
    this.glide = glide;
    this.lifecycle = lifecycle;
    this.treeNode = treeNode;
    this.requestTracker = requestTracker;
    this.context = context;

    connectivityMonitor =
        factory.build(
            context.getApplicationContext(),
            new RequestManagerConnectivityListener(requestTracker));

    // Order matters, this might be unregistered by teh listeners below, so we need to be sure to
    // register first to prevent both assertions and memory leaks.
    glide.registerRequestManager(this);

    // If we're the application level request manager, we may be created on a background thread.
    // In that case we cannot risk synchronously pausing or resuming requests, so we hack around the
    // issue by delaying adding ourselves as a lifecycle listener by posting to the main thread.
    // This should be entirely safe.
    if (Util.isOnBackgroundThread()) {
      Util.postOnUiThread(addSelfToLifecycle);
    } else {
      lifecycle.addListener(this);
    }
    lifecycle.addListener(connectivityMonitor);

    defaultRequestListeners =
        new CopyOnWriteArrayList<>(glide.getGlideContext().getDefaultRequestListeners());
    setRequestOptions(glide.getGlideContext().getDefaultRequestOptions());
  }

  protected synchronized void setRequestOptions(@NonNull RequestOptions toSet) {
    requestOptions = toSet.clone().autoClone();
  }

  private synchronized void updateRequestOptions(@NonNull RequestOptions toUpdate) {
    requestOptions = requestOptions.apply(toUpdate);
  }

  /**
   * Updates the default {@link RequestOptions} for all loads started with this request manager with
   * the given {@link RequestOptions}.
   *
   * <p>The {@link RequestOptions} provided here are applied on top of those provided via {@link
   * GlideBuilder#setDefaultRequestOptions(RequestOptions)}. If there are conflicts, the options
   * applied here will win. Note that this method does not mutate options provided to {@link
   * GlideBuilder#setDefaultRequestOptions(RequestOptions)}.
   *
   * <p>Multiple sets of options can be applied. If there are conflicts the last {@link
   * RequestOptions} applied will win.
   *
   * <p>The modified options will only be applied to loads started after this method is called.
   *
   * @see RequestBuilder#apply(BaseRequestOptions)
   * @return This request manager.
   */
  @NonNull
  public synchronized RequestManager applyDefaultRequestOptions(
      @NonNull RequestOptions requestOptions) {
    updateRequestOptions(requestOptions);
    return this;
  }

  /**
   * Replaces the default {@link RequestOptions} for all loads started with this request manager
   * with the given {@link RequestOptions}.
   *
   * <p>The {@link RequestOptions} provided here replace those that have been previously provided
   * via this method, {@link GlideBuilder#setDefaultRequestOptions(RequestOptions)}, and {@link
   * #applyDefaultRequestOptions(RequestOptions)}.
   *
   * <p>Subsequent calls to {@link #applyDefaultRequestOptions(RequestOptions)} will not mutate the
   * {@link RequestOptions} provided here. Instead the manager will create a clone of these options
   * and mutate the clone.
   *
   * @see #applyDefaultRequestOptions(RequestOptions)
   * @return This request manager.
   */
  @NonNull
  public synchronized RequestManager setDefaultRequestOptions(
      @NonNull RequestOptions requestOptions) {
    setRequestOptions(requestOptions);
    return this;
  }

  /**
   * Clear all resources when onStop() from {@link LifecycleListener} is called.
   *
   * @return This request manager.
   */
  @NonNull
  public synchronized RequestManager clearOnStop() {
    clearOnStop = true;
    return this;
  }

  /**
   * Adds a default {@link RequestListener} that will be added to every request started with this
   * {@link RequestManager}.
   *
   * <p>Multiple {@link RequestListener}s can be added here, in {@link RequestManager} scopes or to
   * individual {@link RequestBuilder}s. {@link RequestListener}s are called in the order they're
   * added. Even if an earlier {@link RequestListener} returns {@code true} from {@link
   * RequestListener#onLoadFailed(GlideException, Object, Target, boolean)} or {@link
   * RequestListener#onResourceReady(Object, Object, Target, DataSource, boolean)}, it will not
   * prevent subsequent {@link RequestListener}s from being called.
   *
   * <p>Because Glide requests can be started for any number of individual resource types, any
   * listener added here has to accept any generic resource type in {@link
   * RequestListener#onResourceReady(Object, Object, Target, DataSource, boolean)}. If you must base
   * the behavior of the listener on the resource type, you will need to use {@code instanceof} to
   * do so. It's not safe to cast resource types without first checking with {@code instanceof}.
   */
  public RequestManager addDefaultRequestListener(RequestListener<Object> requestListener) {
    defaultRequestListeners.add(requestListener);
    return this;
  }

  /**
   * If {@code true} then clear all in-progress and completed requests when the platform sends
   * {@code onTrimMemory} with level = {@code TRIM_MEMORY_MODERATE}.
   */
  public void setPauseAllRequestsOnTrimMemoryModerate(boolean pauseAllOnTrim) {
    pauseAllRequestsOnTrimMemoryModerate = pauseAllOnTrim;
  }

  /**
   * Returns true if loads for this {@link RequestManager} are currently paused.
   *
   * @see #pauseRequests()
   * @see #resumeRequests()
   */
  public synchronized boolean isPaused() {
    return requestTracker.isPaused();
  }

  /**
   * Cancels any in progress loads, but does not clear resources of completed loads.
   *
   * <p>Note #{@link #resumeRequests()} must be called for any requests made before or while the
   * manager is paused to complete. RequestManagers attached to Fragments and Activities
   * automatically resume onStart().
   *
   * @see #isPaused()
   * @see #resumeRequests()
   */
  public synchronized void pauseRequests() {
    requestTracker.pauseRequests();
  }

  /**
   * Cancels any in progress loads and clears resources of completed loads.
   *
   * <p>Note #{@link #resumeRequests()} must be called for any requests made before or while the
   * manager is paused to complete. RequestManagers attached to Fragments and Activities
   * automatically resume onStart().
   *
   * <p>This will release the memory used by completed bitmaps but leaves them in any configured
   * caches. When an #{@link android.app.Activity} receives #{@link
   * android.app.Activity#onTrimMemory(int)} at a level of #{@link
   * android.content.ComponentCallbacks2#TRIM_MEMORY_BACKGROUND} this is desirable in order to keep
   * your process alive longer.
   *
   * @see #isPaused()
   * @see #resumeRequests()
   */
  public synchronized void pauseAllRequests() {
    requestTracker.pauseAllRequests();
  }

  /**
   * Performs {@link #pauseAllRequests()} recursively for all managers that are contextually
   * descendant to this manager based on the Activity/Fragment hierarchy.
   *
   * <p>Similar to {@link #pauseRequestsRecursive()} with the exception that it also clears
   * resources of completed loads.
   */
  // Public API.
  @SuppressWarnings({"WeakerAccess", "unused"})
  public synchronized void pauseAllRequestsRecursive() {
    pauseAllRequests();
    for (RequestManager requestManager : treeNode.getDescendants()) {
      requestManager.pauseAllRequests();
    }
  }

  /**
   * Performs {@link #pauseRequests()} recursively for all managers that are contextually descendant
   * to this manager based on the Activity/Fragment hierarchy:
   *
   * <ul>
   *   <li>When pausing on an Activity all attached fragments will also get paused.
   *   <li>When pausing on an attached Fragment all descendant fragments will also get paused.
   *   <li>When pausing on a detached Fragment or the application context only the current
   *       RequestManager is paused.
   * </ul>
   *
   * <p>Note, on pre-Jelly Bean MR1 calling pause on a Fragment will not cause child fragments to
   * pause, in this case either call pause on the Activity or use a support Fragment.
   */
  // Public API.
  @SuppressWarnings({"WeakerAccess", "unused"})
  public synchronized void pauseRequestsRecursive() {
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
  public synchronized void resumeRequests() {
    requestTracker.resumeRequests();
  }

  /**
   * Performs {@link #resumeRequests()} recursively for all managers that are contextually
   * descendant to this manager based on the Activity/Fragment hierarchy. The hierarchical semantics
   * are identical as for {@link #pauseRequestsRecursive()}.
   */
  // Public API.
  @SuppressWarnings("unused")
  public synchronized void resumeRequestsRecursive() {
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
  public synchronized void onStart() {
    resumeRequests();
    targetTracker.onStart();
  }

  /**
   * Lifecycle callback that unregisters for connectivity events (if the
   * android.permission.ACCESS_NETWORK_STATE permission is present) and pauses in progress loads
   * and clears all resources if {@link #clearOnStop()} is called.
   */
  @Override
  public synchronized void onStop() {
    targetTracker.onStop();
    if (clearOnStop) {
      clearRequests();
    } else {
      pauseRequests();
    }
  }

  /**
   * Lifecycle callback that cancels all in progress requests and clears and recycles resources for
   * all completed requests.
   */
  @Override
  public synchronized void onDestroy() {
    targetTracker.onDestroy();
    clearRequests();
    requestTracker.clearRequests();
    lifecycle.removeListener(this);
    lifecycle.removeListener(connectivityMonitor);
    Util.removeCallbacksOnUiThread(addSelfToLifecycle);
    glide.unregisterRequestManager(this);
  }

  /**
   * Attempts to always load the resource as a {@link android.graphics.Bitmap}, even if it could
   * actually be animated.
   *
   * @return A new request builder for loading a {@link android.graphics.Bitmap}
   */
  @NonNull
  @CheckResult
  public RequestBuilder<Bitmap> asBitmap() {
    return as(Bitmap.class).apply(DECODE_TYPE_BITMAP);
  }

  /**
   * Attempts to always load the resource as a {@link
   * com.bumptech.glide.load.resource.gif.GifDrawable}.
   *
   * <p>If the underlying data is not a GIF, this will fail. As a result, this should only be used
   * if the model represents an animated GIF and the caller wants to interact with the GifDrawable
   * directly. Normally using just {@link #asDrawable()} is sufficient because it will determine
   * whether or not the given data represents an animated GIF and return the appropriate {@link
   * Drawable}, animated or not, automatically.
   *
   * @return A new request builder for loading a {@link
   *     com.bumptech.glide.load.resource.gif.GifDrawable}.
   */
  @NonNull
  @CheckResult
  public RequestBuilder<GifDrawable> asGif() {
    return as(GifDrawable.class).apply(DECODE_TYPE_GIF);
  }

  /**
   * Attempts to always load the resource using any registered {@link
   * com.bumptech.glide.load.ResourceDecoder}s that can decode any subclass of {@link Drawable}.
   *
   * <p>By default, may return either a {@link android.graphics.drawable.BitmapDrawable} or {@link
   * GifDrawable}, but if additional decoders are registered for other {@link Drawable} subclasses,
   * any of those subclasses may also be returned.
   *
   * @return A new request builder for loading a {@link Drawable}.
   */
  @NonNull
  @CheckResult
  public RequestBuilder<Drawable> asDrawable() {
    return as(Drawable.class);
  }

  /**
   * Equivalent to calling {@link #asDrawable()} and then {@link RequestBuilder#load(Bitmap)}.
   *
   * @return A new request builder for loading a {@link Drawable} using the given model.
   */
  @NonNull
  @CheckResult
  @Override
  public RequestBuilder<Drawable> load(@Nullable Bitmap bitmap) {
    return asDrawable().load(bitmap);
  }

  /**
   * Equivalent to calling {@link #asDrawable()} and then {@link RequestBuilder#load(Drawable)}.
   *
   * @return A new request builder for loading a {@link Drawable} using the given model.
   */
  @NonNull
  @CheckResult
  @Override
  public RequestBuilder<Drawable> load(@Nullable Drawable drawable) {
    return asDrawable().load(drawable);
  }

  /**
   * Equivalent to calling {@link #asDrawable()} and then {@link RequestBuilder#load(String)}.
   *
   * @return A new request builder for loading a {@link Drawable} using the given model.
   */
  @NonNull
  @CheckResult
  @Override
  public RequestBuilder<Drawable> load(@Nullable String string) {
    return asDrawable().load(string);
  }

  /**
   * Equivalent to calling {@link #asDrawable()} and then {@link RequestBuilder#load(Uri)}.
   *
   * @return A new request builder for loading a {@link Drawable} using the given model.
   */
  @NonNull
  @CheckResult
  @Override
  public RequestBuilder<Drawable> load(@Nullable Uri uri) {
    return asDrawable().load(uri);
  }

  /**
   * Equivalent to calling {@link #asDrawable()} and then {@link RequestBuilder#load(File)}.
   *
   * @return A new request builder for loading a {@link Drawable} using the given model.
   */
  @NonNull
  @CheckResult
  @Override
  public RequestBuilder<Drawable> load(@Nullable File file) {
    return asDrawable().load(file);
  }

  /**
   * Equivalent to calling {@link #asDrawable()} and then {@link RequestBuilder#load(Integer)}.
   *
   * @return A new request builder for loading a {@link Drawable} using the given model.
   */
  @SuppressWarnings("deprecation")
  @NonNull
  @CheckResult
  @Override
  public RequestBuilder<Drawable> load(@RawRes @DrawableRes @Nullable Integer resourceId) {
    return asDrawable().load(resourceId);
  }

  /**
   * Equivalent to calling {@link #asDrawable()} and then {@link RequestBuilder#load(URL)}.
   *
   * @return A new request builder for loading a {@link Drawable} using the given model.
   */
  @SuppressWarnings("deprecation")
  @CheckResult
  @Override
  @Deprecated
  public RequestBuilder<Drawable> load(@Nullable URL url) {
    return asDrawable().load(url);
  }

  /**
   * Equivalent to calling {@link #asDrawable()} and then {@link RequestBuilder#load(byte[])}.
   *
   * @return A new request builder for loading a {@link Drawable} using the given model.
   */
  @NonNull
  @CheckResult
  @Override
  public RequestBuilder<Drawable> load(@Nullable byte[] model) {
    return asDrawable().load(model);
  }

  /**
   * A helper method equivalent to calling {@link #asDrawable()} and then {@link
   * RequestBuilder#load(Object)} with the given model.
   *
   * @return A new request builder for loading a {@link Drawable} using the given model.
   */
  @NonNull
  @CheckResult
  @Override
  public RequestBuilder<Drawable> load(@Nullable Object model) {
    return asDrawable().load(model);
  }

  /**
   * Attempts always load the resource into the cache and return the {@link File} containing the
   * cached source data.
   *
   * <p>This method is designed to work for remote data that is or will be cached using {@link
   * com.bumptech.glide.load.engine.DiskCacheStrategy#DATA}. As a result, specifying a {@link
   * com.bumptech.glide.load.engine.DiskCacheStrategy} on this request is generally not recommended.
   *
   * @return A new request builder for downloading content to cache and returning the cache File.
   */
  @NonNull
  @CheckResult
  public RequestBuilder<File> downloadOnly() {
    return as(File.class).apply(DOWNLOAD_ONLY_OPTIONS);
  }

  /**
   * A helper method equivalent to calling {@link #downloadOnly()} ()} and then {@link
   * RequestBuilder#load(Object)} with the given model.
   *
   * @return A new request builder for loading a {@link Drawable} using the given model.
   */
  @NonNull
  @CheckResult
  public RequestBuilder<File> download(@Nullable Object model) {
    return downloadOnly().load(model);
  }

  /**
   * Attempts to always load a {@link File} containing the resource, either using a file path
   * obtained from the media store (for local images/videos), or using Glide's disk cache (for
   * remote images/videos).
   *
   * <p>For remote content, prefer {@link #downloadOnly()}.
   *
   * @return A new request builder for obtaining File paths to content.
   */
  @NonNull
  @CheckResult
  public RequestBuilder<File> asFile() {
    return as(File.class).apply(skipMemoryCacheOf(true));
  }

  /**
   * Attempts to load the resource using any registered {@link
   * com.bumptech.glide.load.ResourceDecoder}s that can decode the given resource class or any
   * subclass of the given resource class.
   *
   * @param resourceClass The resource to decode.
   * @return A new request builder for loading the given resource class.
   */
  @NonNull
  @CheckResult
  public <ResourceType> RequestBuilder<ResourceType> as(
      @NonNull Class<ResourceType> resourceClass) {
    return new RequestBuilder<>(glide, this, resourceClass, context);
  }

  /**
   * Cancel any pending loads Glide may have for the view and free any resources that may have been
   * loaded for the view.
   *
   * <p>Note that this will only work if {@link View#setTag(Object)} is not called on this view
   * outside of Glide.
   *
   * @param view The view to cancel loads and free resources for.
   * @throws IllegalArgumentException if an object other than Glide's metadata is put as the view's
   *     tag.
   * @see #clear(Target)
   */
  public void clear(@NonNull View view) {
    clear(new ClearTarget(view));
  }

  /**
   * Cancel any pending loads Glide may have for the target and free any resources (such as {@link
   * Bitmap}s) that may have been loaded for the target so they may be reused.
   *
   * @param target The Target to cancel loads for.
   */
  public void clear(@Nullable final Target<?> target) {
    if (target == null) {
      return;
    }

    untrackOrDelegate(target);
  }

  private void untrackOrDelegate(@NonNull Target<?> target) {
    boolean isOwnedByUs = untrack(target);
    // We'll end up here if the Target was cleared after the RequestManager that started the request
    // is destroyed. That can happen for at least two reasons:
    // 1. We call clear() on a background thread using something other than Application Context
    // RequestManager.
    // 2. The caller retains a reference to the RequestManager after the corresponding Activity or
    // Fragment is destroyed, starts a load with it, and then clears that load with a different
    // RequestManager. Callers seem especially likely to do this in retained Fragments (#2262).
    //
    // #1 is always an error. At best the caller is leaking memory briefly in something like an
    // AsyncTask. At worst the caller is leaking an Activity or Fragment for a sustained period of
    // time if they do something like reference the Activity RequestManager in a long lived
    // background thread or task.
    //
    // #2 is always an error. Callers shouldn't be starting new loads using RequestManagers after
    // the corresponding Activity or Fragment is destroyed because retaining any reference to the
    // RequestManager leaks memory. It's possible that there's some brief period of time during or
    // immediately after onDestroy where this is reasonable, but I can't think of why.
    Request request = target.getRequest();
    if (!isOwnedByUs && !glide.removeFromManagers(target) && request != null) {
      target.setRequest(null);
      request.clear();
    }
  }

  synchronized boolean untrack(@NonNull Target<?> target) {
    Request request = target.getRequest();
    // If the Target doesn't have a request, it's already been cleared.
    if (request == null) {
      return true;
    }

    if (requestTracker.clearAndRemove(request)) {
      targetTracker.untrack(target);
      target.setRequest(null);
      return true;
    } else {
      return false;
    }
  }

  synchronized void track(@NonNull Target<?> target, @NonNull Request request) {
    targetTracker.track(target);
    requestTracker.runRequest(request);
  }

  List<RequestListener<Object>> getDefaultRequestListeners() {
    return defaultRequestListeners;
  }

  synchronized RequestOptions getDefaultRequestOptions() {
    return requestOptions;
  }

  @NonNull
  <T> TransitionOptions<?, T> getDefaultTransitionOptions(Class<T> transcodeClass) {
    return glide.getGlideContext().getDefaultTransitionOptions(transcodeClass);
  }

  @Override
  public synchronized String toString() {
    return super.toString() + "{tracker=" + requestTracker + ", treeNode=" + treeNode + "}";
  }

  @Override
  public void onTrimMemory(int level) {
    if (level == TRIM_MEMORY_MODERATE && pauseAllRequestsOnTrimMemoryModerate) {
      pauseAllRequestsRecursive();
    }
  }

  @Override
  public void onLowMemory() {
    // Nothing to add conditionally. See Glide#onTrimMemory for unconditional behavior.
  }

  private synchronized void clearRequests() {
    for (Target<?> target : targetTracker.getAll()) {
      clear(target);
    }
    targetTracker.clear();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {}

  private class RequestManagerConnectivityListener
      implements ConnectivityMonitor.ConnectivityListener {
    @GuardedBy("RequestManager.this")
    private final RequestTracker requestTracker;

    RequestManagerConnectivityListener(@NonNull RequestTracker requestTracker) {
      this.requestTracker = requestTracker;
    }

    @Override
    public void onConnectivityChanged(boolean isConnected) {
      if (isConnected) {
        synchronized (RequestManager.this) {
          requestTracker.restartRequests();
        }
      }
    }
  }

  private static class ClearTarget extends CustomViewTarget<View, Object> {

    ClearTarget(@NonNull View view) {
      super(view);
    }

    @Override
    protected void onResourceCleared(@Nullable Drawable placeholder) {
      // Do nothing, we don't retain a reference to our resource.
    }

    @Override
    public void onLoadFailed(@Nullable Drawable errorDrawable) {
      // Do nothing.
    }

    @Override
    public void onResourceReady(
        @NonNull Object resource, @Nullable Transition<? super Object> transition) {
      // Do nothing.
    }
  }
}
