package com.bumptech.glide;

import static com.bumptech.glide.request.RequestOptions.signatureOf;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ImageView;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.BaseRequestOptions;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestCoordinator;
import com.bumptech.glide.request.RequestFutureTarget;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.SingleRequest;
import com.bumptech.glide.request.ThumbnailRequestCoordinator;
import com.bumptech.glide.request.target.PreloadTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.ApplicationVersionSignature;
import com.bumptech.glide.signature.ObjectKey;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Util;
import java.io.File;
import java.net.URL;
import java.util.UUID;

/**
 * A generic class that can handle setting options and staring loads for generic resource types.
 *
 * @param <TranscodeType> The type of resource that will be delivered to the
 * {@link com.bumptech.glide.request.target.Target}.
 */
public class RequestBuilder<TranscodeType> implements Cloneable {
  private static final TransitionOptions<?, ?> DEFAULT_ANIMATION_OPTIONS =
      new GenericTransitionOptions<Object>();
  private static final BaseRequestOptions<?> DOWNLOAD_ONLY_OPTIONS =
      new RequestOptions().diskCacheStrategy(DiskCacheStrategy.DATA).priority(Priority.LOW)
          .skipMemoryCache(true);

  private final GlideContext context;
  private final RequestManager requestManager;
  private final Class<TranscodeType> transcodeClass;
  private final BaseRequestOptions<?> defaultRequestOptions;

  @NonNull private BaseRequestOptions<?> requestOptions;
  @SuppressWarnings("unchecked")
  private TransitionOptions<?, ? super TranscodeType> transitionOptions =
      (TransitionOptions<?, ? super TranscodeType>) DEFAULT_ANIMATION_OPTIONS;

  @Nullable private Object model;
  // model may occasionally be null, so to enforce that load() was called, put a boolean rather
  // than relying on model not to be null.
  @Nullable private RequestListener<TranscodeType> requestListener;
  @Nullable private RequestBuilder<TranscodeType> thumbnailBuilder;
  @Nullable private Float thumbSizeMultiplier;
  private boolean isModelSet;
  private boolean isThumbnailBuilt;

  RequestBuilder(Class<TranscodeType> transcodeClass, RequestBuilder<?> other) {
    this(other.context, other.requestManager, transcodeClass);
    model = other.model;
    isModelSet = other.isModelSet;
    requestOptions = other.requestOptions;
  }

  RequestBuilder(GlideContext context, RequestManager requestManager,
      Class<TranscodeType> transcodeClass) {
    this.requestManager = requestManager;
    this.context = Preconditions.checkNotNull(context);
    this.transcodeClass = transcodeClass;

    this.defaultRequestOptions = requestManager.getDefaultRequestOptions();
    this.requestOptions = defaultRequestOptions;
  }

  /**
   * Applies the given options to the request, options set or unset in the given options will
   * replace those previously set in options in this class.
   *
   * @see BaseRequestOptions#apply(BaseRequestOptions)
   * @return This request builder.
   */
  public RequestBuilder<TranscodeType> apply(@NonNull BaseRequestOptions<?> requestOptions) {
    Preconditions.checkNotNull(requestOptions);
    BaseRequestOptions<?> toMutate = defaultRequestOptions == this.requestOptions
        ? this.requestOptions.clone() : this.requestOptions;
    this.requestOptions = toMutate.apply(requestOptions);
    return this;
  }

  /**
   * Sets the {@link TransitionOptions} to use to transition from the placeholder or thumbnail when
   * this load completes.
   *
   * <p>The given {@link TransitionOptions} will replace any {@link TransitionOptions} set
   * previously.
   *
   * @return This request builder.
   */
  public RequestBuilder<TranscodeType> transition(
      @NonNull TransitionOptions<?, ? super TranscodeType> transitionOptions) {
    this.transitionOptions = Preconditions.checkNotNull(transitionOptions);
    return this;
  }

  /**
   * Sets a RequestBuilder listener to monitor the resource load. It's best to create a single
   * instance of an exception handler per type of request (usually activity/fragment) rather than
   * pass one in per request to avoid some redundant object allocation.
   *
   * @param requestListener The request listener to use.
   * @return This request builder.
   */
  @SuppressWarnings("unchecked")
  public RequestBuilder<TranscodeType> listener(
      @Nullable RequestListener<TranscodeType> requestListener) {
    this.requestListener = requestListener;

    return this;
  }

  /**
   * Loads and displays the resource retrieved by the given thumbnail request if it finishes before
   * this request. Best used for loading thumbnail resources that are smaller and will be loaded
   * more quickly than the full size resource. There are no guarantees about the order in which the
   * requests will actually finish. However, if the thumb request completes after the full request,
   * the thumb resource will never replace the full resource.
   *
   * @param thumbnailRequest The request to use to load the thumbnail.
   * @return This request builder.
   * @see #thumbnail(float)
   *
   * <p> Recursive calls to thumbnail are supported. </p>
   */
  @SuppressWarnings("unchecked")
  public RequestBuilder<TranscodeType> thumbnail(
      @Nullable RequestBuilder<TranscodeType> thumbnailRequest) {
    this.thumbnailBuilder = thumbnailRequest;

    return this;
  }

  /**
   * Loads a resource in an identical manner to this request except with the dimensions of the
   * target multiplied by the given size multiplier. If the thumbnail load completes before the full
   * size load, the thumbnail will be shown. If the thumbnail load completes after the full size
   * load, the thumbnail will not be shown.
   *
   * <p> Note - The thumbnail resource will be smaller than the size requested so the target (or
   * {@link ImageView}) must be able to scale the thumbnail appropriately. See
   * {@link android.widget.ImageView.ScaleType}. </p>
   *
   * <p> Almost all options will be copied from the original load, including the {@link
   * com.bumptech.glide.load.model.ModelLoader}, {@link com.bumptech.glide.load.ResourceDecoder},
   * and {@link com.bumptech.glide.load.Transformation}s. However,
   * {@link com.bumptech.glide.request.BaseRequestOptions#placeholder(int)} and
   * {@link com.bumptech.glide.request.BaseRequestOptions#error(int)}, and
   * {@link #listener(RequestListener)} will only be used on the full size load and will not be
   * copied for the thumbnail load. </p>
   *
   * <p> Recursive calls to thumbnail are supported. </p>
   *
   * @param sizeMultiplier The multiplier to apply to the {@link Target}'s dimensions when loading
   *                       the thumbnail.
   * @return This request builder.
   */
  @SuppressWarnings("unchecked")
  public RequestBuilder<TranscodeType> thumbnail(float sizeMultiplier) {
    if (sizeMultiplier < 0f || sizeMultiplier > 1f) {
      throw new IllegalArgumentException("sizeMultiplier must be between 0 and 1");
    }
    this.thumbSizeMultiplier = sizeMultiplier;

    return this;
  }

  /**
   * Sets the specific model to load data for.
   *
   * <p> This method must be called at least once before
   * {@link #into(com.bumptech.glide.request.target.Target)} is called. </p>
   *
   * @param model The model to load data for, or null.
   * @return This request builder.
   */
  @SuppressWarnings("unchecked")
  public RequestBuilder<TranscodeType> load(@Nullable Object model) {
    return loadGeneric(model);
  }

  private RequestBuilder<TranscodeType> loadGeneric(@Nullable Object model) {
    this.model = model;
    isModelSet = true;
    return this;
  }

  /**
   * Returns a request builder to load the given {@link java.lang.String}. signature.
   *
   * <p> Note - this method caches data using only the given String as the cache key. If the data is
   * a Uri outside of your control, or you otherwise expect the data represented by the given String
   * to change without the String identifier changing, Consider using
   * {@link com.bumptech.glide.request.BaseRequestOptions#signature(com.bumptech.glide.load.Key)} to
   * mixin a signature you create that identifies the data currently at the given String that will
   * invalidate the cache if that data changes. Alternatively, using
   * {@link com.bumptech.glide.load.engine.DiskCacheStrategy#NONE} and/or
   * {@link com.bumptech.glide.request.BaseRequestOptions#skipMemoryCache(boolean)} may be
   * appropriate.
   * </p>
   *
   * @see #load(Object)
   *
   * @param string A file path, or a uri or url handled by
   * {@link com.bumptech.glide.load.model.UriLoader}.
   */
  public RequestBuilder<TranscodeType> load(@Nullable String string) {
    return loadGeneric(string);
  }

  /**
   * Returns a request builder to load the given {@link Uri}.
   *
   * <p> Note - this method caches data at Uris using only the Uri itself as the cache key. The data
   * represented by Uris from some content providers may change without the Uri changing, which
   * means using this method can lead to displaying stale data. Consider using
   * {@link com.bumptech.glide.request.BaseRequestOptions#signature(com.bumptech.glide.load.Key)} to
   * mixin a signature you create based on the data at the given Uri that will invalidate the cache
   * if that data changes. Alternatively, using
   * {@link com.bumptech.glide.load.engine.DiskCacheStrategy#NONE} and/or
   * {@link com.bumptech.glide.request.BaseRequestOptions#skipMemoryCache(boolean)} may be
   * appropriate. </p>
   *
   * @see #load(Object)
   *
   * @param uri The Uri representing the image. Must be of a type handled by
   * {@link com.bumptech.glide.load.model.UriLoader}.
   */
  public RequestBuilder<TranscodeType> load(@Nullable Uri uri) {
    return loadGeneric(uri);
  }

  /**
   * Returns a request builder to load the given {@link File}.
   *
   * <p> Note - this method caches data for Files using only the file path itself as the cache key.
   * The data in the File can change so using this method can lead to displaying stale data. If you
   * expect the data in the File to change, Consider using
   * {@link com.bumptech.glide.request.BaseRequestOptions#signature(com.bumptech.glide.load.Key)}
   * to mixin a signature you create that identifies the data currently in the File that will
   * invalidate the cache if that data changes. Alternatively, using
   * {@link com.bumptech.glide.load.engine.DiskCacheStrategy#NONE} and/or
   * {@link com.bumptech.glide.request.BaseRequestOptions#skipMemoryCache(boolean)} may be
   * appropriate.
   * </p>
   *
   * @see #load(Object)
   *
   * @param file The File containing the image
   */
  public RequestBuilder<TranscodeType> load(@Nullable File file) {
    return loadGeneric(file);
  }

  /**
   * Returns a request builder to load the given resource id. Returns a request builder that uses
   * the {@link com.bumptech.glide.load.model.ModelLoaderFactory} currently registered or
   * {@link Integer} to load the image represented by the given {@link Integer} resource id.
   * Defaults to {@link com.bumptech.glide.load.model.ResourceLoader} to load resource id models.
   *
   * <p> By default this method adds a version code based signature to the cache key used to cache
   * this resource in Glide. This signature is sufficient to guarantee that end users will see the
   * most up to date versions of your Drawables, but during development if you do not increment your
   * version code before each install and you replace a Drawable with different data without
   * changing the Drawable name, you may see inconsistent cached data. To get around this, consider
   * using {@link com.bumptech.glide.load.engine.DiskCacheStrategy#NONE} via
   * {@link BaseRequestOptions#diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy)}
   * during development, and re-enabling the default
   * {@link com.bumptech.glide.load.engine.DiskCacheStrategy#RESOURCE} for release builds. </p>
   *
   * @see #load(Integer)
   * @see com.bumptech.glide.signature.ApplicationVersionSignature
   */
  public RequestBuilder<TranscodeType> load(@Nullable Integer resourceId) {
    return loadGeneric(resourceId).apply(signatureOf(ApplicationVersionSignature.obtain(context)));
  }

  /**
   * Returns a request builder to load the given {@link URL}.
   *
   * @param url The URL representing the image.
   * @see #load(Object)
   * @deprecated The {@link java.net.URL} class has <a href="http://goo.gl/c4hHNu">a number of
   * performance problems</a> and should generally be avoided when possible. Prefer
   * {@link #load(android.net.Uri)} or {@link #load(String)}.
   */
  @Deprecated
  public RequestBuilder<TranscodeType> load(@Nullable URL url) {
    return loadGeneric(url);
  }

  /**
   * Returns a request to load the given byte array.
   *
   * <p> Note - by default loads for bytes are not cached in either the memory or the disk cache.
   * </p>
   *
   * @param model the data to load.
   * @see #load(Object)
   */
  public RequestBuilder<TranscodeType> load(@Nullable byte[] model) {
    return loadGeneric(model).apply(signatureOf(new ObjectKey(UUID.randomUUID().toString()))
        .diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true /*skipMemoryCache*/));
  }

  /**
   * Returns a copy of this request builder with all of the options put so far on this builder.
   *
   * <p> This method returns a "deep" copy in that all non-immutable arguments are copied such that
   * changes to one builder will not affect the other builder. However, in addition to immutable
   * arguments, the current model is not copied copied so changes to the model will affect both
   * builders. </p>
   */
  @SuppressWarnings("unchecked")
  @Override
  public RequestBuilder<TranscodeType> clone() {
    try {
      RequestBuilder<TranscodeType> result = (RequestBuilder<TranscodeType>) super.clone();
      result.requestOptions = result.requestOptions.clone();
      result.transitionOptions = result.transitionOptions.clone();
      return result;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Set the target the resource will be loaded into.
   *
   * @param target The target to load the resource into.
   * @return The given target.
   * @see RequestManager#clear(Target)
   */
  public <Y extends Target<TranscodeType>> Y into(@NonNull Y target) {
    Util.assertMainThread();
    Preconditions.checkNotNull(target);
    if (!isModelSet) {
      throw new IllegalArgumentException("You must call #load() before calling #into()");
    }

    Request previous = target.getRequest();

    if (previous != null) {
      requestManager.clear(target);
    }

    requestOptions.lock();
    Request request = buildRequest(target);
    target.setRequest(request);
    requestManager.track(target, request);

    return target;
  }

  /**
   * Sets the {@link ImageView} the resource will be loaded into, cancels any existing loads into
   * the view, and frees any resources Glide may have previously loaded into the view so they may be
   * reused.
   *
   * @see RequestManager#clear(Target)
   *
   * @param view The view to cancel previous loads for and load the new resource into.
   * @return The
   * {@link com.bumptech.glide.request.target.Target} used to wrap the given {@link ImageView}.
   */
  public Target<TranscodeType> into(ImageView view) {
    Util.assertMainThread();
    Preconditions.checkNotNull(view);

    if (!requestOptions.isTransformationSet()
        && requestOptions.isTransformationAllowed()
        && view.getScaleType() != null) {
      if (requestOptions.isLocked()) {
        requestOptions = requestOptions.clone();
      }
      switch (view.getScaleType()) {
        case CENTER_CROP:
          requestOptions.optionalCenterCrop(context);
          break;
        case CENTER_INSIDE:
          requestOptions.optionalCenterInside(context);
          break;
        case FIT_CENTER:
        case FIT_START:
        case FIT_END:
          requestOptions.optionalFitCenter(context);
          break;
        //$CASES-OMITTED$
        default:
          // Do nothing.
      }
    }

    return into(context.buildImageViewTarget(view, transcodeClass));
  }

  /**
   * Returns a future that can be used to do a blocking get on a background thread.
   *
   * @param width  The desired width in pixels, or {@link Target#SIZE_ORIGINAL}. This will be
   *               overridden by
   *               {@link com.bumptech.glide.request.BaseRequestOptions#override(int, int)} if
   *               previously called.
   * @param height The desired height in pixels, or {@link Target#SIZE_ORIGINAL}. This will be
   *               overridden by
   *               {@link com.bumptech.glide.request.BaseRequestOptions#override(int, int)}} if
   *               previously called).
   * @see RequestManager#clear(Target)
   *
   * @deprecated Use {@link #submit(int, int)} instead.
   */
  @Deprecated
  public FutureTarget<TranscodeType> into(int width, int height) {
    return submit(width, height);
  }

  /**
   * Returns a future that can be used to do a blocking get on a background thread.
   *
   * <p>This method defaults to {@link Target#SIZE_ORIGINAL} for the width and the height. However,
   * since the width and height will be overridden by values passed to {@link
   * RequestOptions#override(int, int)}, this method can be used whenever {@link RequestOptions}
   * with override values are applied, or whenever you want to retrieve the image in its original
   * size.
   *
   * @see #submit(int, int)
   * @see #into(Target)
   */
  public FutureTarget<TranscodeType> submit() {
    return submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
  }

  /**
   * Returns a future that can be used to do a blocking get on a background thread.
   *
   * @param width  The desired width in pixels, or {@link Target#SIZE_ORIGINAL}. This will be
   *               overridden by
   *               {@link com.bumptech.glide.request.BaseRequestOptions#override(int, int)} if
   *               previously called.
   * @param height The desired height in pixels, or {@link Target#SIZE_ORIGINAL}. This will be
   *               overridden by
   *               {@link com.bumptech.glide.request.BaseRequestOptions#override(int, int)}} if
   *               previously called).
   */
  public FutureTarget<TranscodeType> submit(int width, int height) {
    final RequestFutureTarget<TranscodeType> target =
        new RequestFutureTarget<>(context.getMainHandler(), width, height);

    if (Util.isOnBackgroundThread()) {
      context.getMainHandler().post(new Runnable() {
        @Override
        public void run() {
          if (!target.isCancelled()) {
            into(target);
          }
        }
      });
    } else {
      into(target);
    }

    return target;
  }

  /**
   * Preloads the resource into the cache using the given width and height.
   *
   * <p> Pre-loading is useful for making sure that resources you are going to to want in the near
   * future are available quickly. </p>
   *
   * @param width  The desired width in pixels, or {@link Target#SIZE_ORIGINAL}. This will be
   *               overridden by
   *               {@link com.bumptech.glide.request.BaseRequestOptions#override(int, int)} if
   *               previously called.
   * @param height The desired height in pixels, or {@link Target#SIZE_ORIGINAL}. This will be
   *               overridden by
   *               {@link com.bumptech.glide.request.BaseRequestOptions#override(int, int)}} if
   *               previously called).
   * @return A {@link Target} that can be used to cancel the load via
   * {@link RequestManager#clear(Target)}.
   * @see com.bumptech.glide.ListPreloader
   */
  public Target<TranscodeType> preload(int width, int height) {
    final PreloadTarget<TranscodeType> target = PreloadTarget.obtain(requestManager, width, height);
    return into(target);
  }

  /**
   * Preloads the resource into the cache using {@link Target#SIZE_ORIGINAL} as the target width and
   * height. Equivalent to calling {@link #preload(int, int)} with {@link Target#SIZE_ORIGINAL} as
   * the width and height.
   *
   * @return A {@link Target} that can be used to cancel the load via
   * {@link RequestManager#clear(Target)}
   * @see #preload(int, int)
   */
  public Target<TranscodeType> preload() {
    return preload(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
  }

  /**
   * Loads the original unmodified data into the cache and calls the given Target with the cache
   * File.
   *
   * @param target The Target that will receive the cache File when the load completes
   * @param <Y>    The type of Target.
   * @return The given Target.
   *
   * @deprecated Use {@link RequestManager#downloadOnly()} and {@link #into(Target)}.
   */
  @Deprecated
  public <Y extends Target<File>> Y downloadOnly(Y target) {
    return getDownloadOnlyRequest().into(target);
  }

  /**
   * Loads the original unmodified data into the cache and returns a
   * {@link java.util.concurrent.Future} that can be used to retrieve the cache File containing the
   * data.
   *
   * @param width  The width in pixels to use to fetch the data.
   * @param height The height in pixels to use to fetch the data.
   * @return A {@link java.util.concurrent.Future} that can be used to retrieve the cache File
   * containing the data.
   *
   * @deprecated Use {@link RequestManager#downloadOnly()} and {@link #into(int, int)}.
   */
  @Deprecated
  public FutureTarget<File> downloadOnly(int width, int height) {
    return getDownloadOnlyRequest().submit(width, height);
  }

  private RequestBuilder<File> getDownloadOnlyRequest() {
    return new RequestBuilder<>(File.class, this).apply(DOWNLOAD_ONLY_OPTIONS);
  }

  private Priority getThumbnailPriority(Priority current) {
    switch (current) {
      case LOW:
        return Priority.NORMAL;
      case NORMAL:
        return Priority.HIGH;
      case HIGH:
      case IMMEDIATE:
        return Priority.IMMEDIATE;
      default:
        throw new IllegalArgumentException("unknown priority: " + requestOptions.getPriority());
    }
  }

  private Request buildRequest(Target<TranscodeType> target) {
    return buildRequestRecursive(target, null, transitionOptions, requestOptions.getPriority(),
        requestOptions.getOverrideWidth(), requestOptions.getOverrideHeight());
  }

  private Request buildRequestRecursive(Target<TranscodeType> target,
      @Nullable ThumbnailRequestCoordinator parentCoordinator,
      TransitionOptions<?, ? super TranscodeType> transitionOptions,
      Priority priority, int overrideWidth, int overrideHeight) {
    if (thumbnailBuilder != null) {
      // Recursive case: contains a potentially recursive thumbnail request builder.
      if (isThumbnailBuilt) {
        throw new IllegalStateException("You cannot use a request as both the main request and a "
            + "thumbnail, consider using clone() on the request(s) passed to thumbnail()");
      }

      TransitionOptions<?, ? super TranscodeType> thumbTransitionOptions =
          thumbnailBuilder.transitionOptions;
      if (DEFAULT_ANIMATION_OPTIONS.equals(thumbTransitionOptions)) {
        thumbTransitionOptions = transitionOptions;
      }

      Priority thumbPriority = thumbnailBuilder.requestOptions.isPrioritySet()
          ? thumbnailBuilder.requestOptions.getPriority() : getThumbnailPriority(priority);

      int thumbOverrideWidth = thumbnailBuilder.requestOptions.getOverrideWidth();
      int thumbOverrideHeight = thumbnailBuilder.requestOptions.getOverrideHeight();
      if (Util.isValidDimensions(overrideWidth, overrideHeight)
          && !thumbnailBuilder.requestOptions.isValidOverride()) {
        thumbOverrideWidth = requestOptions.getOverrideWidth();
        thumbOverrideHeight = requestOptions.getOverrideHeight();
      }

      ThumbnailRequestCoordinator coordinator = new ThumbnailRequestCoordinator(parentCoordinator);
      Request fullRequest = obtainRequest(target, requestOptions, coordinator,
          transitionOptions, priority, overrideWidth, overrideHeight);
      isThumbnailBuilt = true;
      // Recursively generate thumbnail requests.
      Request thumbRequest = thumbnailBuilder.buildRequestRecursive(target, coordinator,
          thumbTransitionOptions, thumbPriority, thumbOverrideWidth, thumbOverrideHeight);
      isThumbnailBuilt = false;
      coordinator.setRequests(fullRequest, thumbRequest);
      return coordinator;
    } else if (thumbSizeMultiplier != null) {
      // Base case: thumbnail multiplier generates a thumbnail request, but cannot recurse.
      ThumbnailRequestCoordinator coordinator = new ThumbnailRequestCoordinator(parentCoordinator);
      Request fullRequest = obtainRequest(target, requestOptions, coordinator, transitionOptions,
          priority, overrideWidth, overrideHeight);
      BaseRequestOptions<?> thumbnailOptions = requestOptions.clone()
          .sizeMultiplier(thumbSizeMultiplier);

      Request thumbnailRequest = obtainRequest(target, thumbnailOptions, coordinator,
          transitionOptions, getThumbnailPriority(priority), overrideWidth, overrideHeight);

      coordinator.setRequests(fullRequest, thumbnailRequest);
      return coordinator;
    } else {
      // Base case: no thumbnail.
      return obtainRequest(target, requestOptions, parentCoordinator, transitionOptions, priority,
          overrideWidth, overrideHeight);
    }
  }

  private Request obtainRequest(Target<TranscodeType> target,
      BaseRequestOptions<?> requestOptions, RequestCoordinator requestCoordinator,
      TransitionOptions<?, ? super TranscodeType> transitionOptions, Priority priority,
      int overrideWidth, int overrideHeight) {
    requestOptions.lock();

    return SingleRequest.obtain(
        context,
        model,
        transcodeClass,
        requestOptions,
        overrideWidth,
        overrideHeight,
        priority,
        target,
        requestListener,
        requestCoordinator,
        context.getEngine(),
        transitionOptions.getTransitionFactory());
  }
}
