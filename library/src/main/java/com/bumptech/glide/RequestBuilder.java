package com.bumptech.glide;

import static com.bumptech.glide.request.RequestOptions.signatureOf;

import android.net.Uri;
import android.widget.ImageView;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.RequestContext;
import com.bumptech.glide.load.resource.transcode.BitmapDrawableTranscoder;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.load.resource.transcode.UnitTranscoder;
import com.bumptech.glide.manager.Lifecycle;
import com.bumptech.glide.manager.RequestTracker;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.GlideContext;
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
import com.bumptech.glide.signature.StringSignature;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Util;

import java.io.File;
import java.net.URL;
import java.util.UUID;

/**
 * A generic class that can handle setting options and staring loads for generic resource types.
 *
 * @param <ResourceType> The type of the resource that will be loaded.
 * @param <TranscodeType> The type of resource the decoded resource will be transcoded to.
 */
public class RequestBuilder<ResourceType, TranscodeType> implements Cloneable {
    private static final RequestOptions DEFAULT_REQUEST_OPTIONS = new RequestOptions();
    private static final TransformationOptions<?, ?> DEFAULT_TRANSFORMATION_OPTIONS = new GenericTransformationOptions<Object>();
    private static final AnimationOptions<?, ?> DEFAULT_ANIMATION_OPTIONS =
            new GenericAnimationOptions<Object>();
    protected final GlideContext context;
    protected final Class<ResourceType> resourceClass;
    protected final Class<TranscodeType> transcodeClass;
    protected final RequestTracker requestTracker;
    protected final Lifecycle lifecycle;

    private RequestOptions requestOptions = DEFAULT_REQUEST_OPTIONS;
    @SuppressWarnings("unchecked")
    private TransformationOptions<?, ResourceType> transformationOptions =
            (TransformationOptions<?, ResourceType>) DEFAULT_TRANSFORMATION_OPTIONS;
    @SuppressWarnings("unchecked")
    private AnimationOptions<?, ? super TranscodeType> animationOptions =
            (AnimationOptions<?, ? super TranscodeType>) DEFAULT_ANIMATION_OPTIONS;

    private Object model;
    // model may occasionally be null, so to enforce that load() was called, set a boolean rather than relying on model
    // not to be null.
    private boolean isModelSet;
    private RequestListener<TranscodeType> requestListener;
    private ResourceTranscoder<ResourceType, ? extends TranscodeType> transcoder;
    private RequestBuilder<?, TranscodeType> thumbnailBuilder;
    private Float thumbSizeMultiplier;

    RequestBuilder(Class<ResourceType> resourceClass, Class<TranscodeType> transcodeClass, RequestBuilder<?, ?> other) {
        this(other.context, resourceClass, transcodeClass, other.requestTracker, other.lifecycle);
        this.model = other.model;
        this.isModelSet = other.isModelSet;
        this.requestOptions = other.requestOptions;
    }

    RequestBuilder(GlideContext context, Class<ResourceType> resourceClass, Class<TranscodeType> transcodeClass,
            RequestTracker requestTracker, Lifecycle lifecycle) {
        this.context = Preconditions.checkNotNull(context);
        this.resourceClass = resourceClass;
        this.transcodeClass = transcodeClass;
        this.requestTracker = requestTracker;
        this.lifecycle = lifecycle;
    }

    public RequestBuilder<ResourceType, TranscodeType> apply(RequestOptions requestOptions) {
        Preconditions.checkNotNull(requestOptions);
        this.requestOptions = DEFAULT_REQUEST_OPTIONS.equals(this.requestOptions)
                ? requestOptions : this.requestOptions.apply(requestOptions);
        return this;
    }

    public RequestBuilder<ResourceType, TranscodeType> animate(
            AnimationOptions<?, ? super TranscodeType> animationOptions) {
        this.animationOptions = Preconditions.checkNotNull(animationOptions);
        return this;
    }

    public RequestBuilder<ResourceType, TranscodeType> transform(
            TransformationOptions<?, ResourceType> transformationOptions) {
        Preconditions.checkNotNull(transformationOptions);
        this.transformationOptions = DEFAULT_TRANSFORMATION_OPTIONS.equals(this.transformationOptions)
                ? transformationOptions : this.transformationOptions.apply(transformationOptions);
        return this;
    }

     /**
     * Sets a RequestBuilder listener to monitor the resource load. It's best to create a single instance of an
     * exception handler per type of request (usually activity/fragment) rather than pass one in per request to
     * avoid some redundant object allocation.
     *
     * @param requestListener The request listener to use.
     * @return This request builder.
     */
     @SuppressWarnings("unchecked")
    public RequestBuilder<ResourceType, TranscodeType> listener(RequestListener<TranscodeType> requestListener) {
        this.requestListener = requestListener;

        return this;
    }

    /**
     * Sets the {@link ResourceTranscoder} to use for this load.
     *
     * @see UnitTranscoder
     * @see BitmapDrawableTranscoder
     *
     * @param transcoder The transcoder to use.
     * @return This request builder.
     */
    @SuppressWarnings("unchecked")
    public RequestBuilder<ResourceType, TranscodeType> transcoder(
            ResourceTranscoder<ResourceType, ? extends TranscodeType> transcoder) {
        this.transcoder = transcoder;
        return this;
    }

    /**
     * Loads and displays the resource retrieved by the given thumbnail request if it finishes before this request.
     * Best used for loading thumbnail resources that are smaller and will be loaded more quickly than the full size
     * resource. There are no guarantees about the order in which the requests will actually finish. However, if the
     * thumb request completes after the full request, the thumb resource will never replace the full resource.
     *
     * @see #thumbnail(float)
     *
     * <p>
     *     Recursive calls to thumbnail are supported.
     * </p>
     *
     * @param thumbnailRequest The request to use to load the thumbnail.
     * @return This request builder.
     */
    @SuppressWarnings("unchecked")
    public RequestBuilder<ResourceType, TranscodeType> thumbnail(RequestBuilder<?, TranscodeType> thumbnailRequest) {
        this.thumbnailBuilder = thumbnailRequest;

        return this;
    }

    /**
     * Loads a resource in an identical manner to this request except with the dimensions of the target multiplied
     * by the given size multiplier. If the thumbnail load completes before the fullsize load, the thumbnail will
     * be shown. If the thumbnail load completes afer the fullsize load, the thumbnail will not be shown.
     *
     * <p>
     *     Note - The thumbnail resource will be smaller than the size requested so the target (or {@link ImageView})
     *     must be able to scale the thumbnail appropriately. See {@link android.widget.ImageView.ScaleType}.
     * </p>
     *
     * <p>
     *     Almost all options will be copied from the original load, including the
     *     {@link com.bumptech.glide.load.model.ModelLoader}, {@link com.bumptech.glide.load.ResourceDecoder}, and
     *     {@link Transformation}s. However, {@link RequestOptions#placeholder(int)} and
     *     {@link RequestOptions#error(int)}, and {@link #listener(RequestListener)} will only be used on the fullsize
     *     load and will not be copied for the thumbnail load.
     * </p>
     *
     * <p>
     *     Recursive calls to thumbnail are supported.
     * </p>
     *
     * @param sizeMultiplier The multiplier to apply to the {@link Target}'s dimensions when loading the thumbnail.
     * @return This request builder.
     */
    @SuppressWarnings("unchecked")
    public RequestBuilder<ResourceType, TranscodeType> thumbnail(float sizeMultiplier) {
        if (sizeMultiplier < 0f || sizeMultiplier > 1f) {
            throw new IllegalArgumentException("sizeMultiplier must be between 0 and 1");
        }
        this.thumbSizeMultiplier = sizeMultiplier;

        return this;
    }


    /**
     * Sets the specific model to load data for.
     *
     * <p>
     *      This method must be called at least once before {@link #into(com.bumptech.glide.request.target.Target)} is
     *      called.
     * </p>
     *
     * @param model The model to load data for, or null.
     * @return This request builder.
     */
    @SuppressWarnings("unchecked")
    public RequestBuilder<ResourceType, TranscodeType> load(Object model) {
        return loadGeneric(model);
    }

    private RequestBuilder<ResourceType, TranscodeType> loadGeneric(Object model) {
        this.model = model;
        isModelSet = true;
        return this;
    }

    /**
     * Returns a request builder to load the given {@link java.lang.String}.
     * signature.
     *
     * <p>
     *     Note - this method caches data using only the given String as the cache key. If the data is a Uri outside of
     *     your control, or you otherwise expect the data represented by the given String to change without the String
     *     identifier changing, Consider using
     *     {@link RequestOptions#signature(Key)} to mixin a signature
     *     you create that identifies the data currently at the given String that will invalidate the cache if that data
     *     changes. Alternatively, using {@link com.bumptech.glide.load.engine.DiskCacheStrategy#NONE} and/or
     *     {@link RequestOptions#skipMemoryCache(boolean)} may be appropriate.
     * </p>
     *
     * @see #load(Object)
     *
     * @param string A file path, or a uri or url handled by {@link com.bumptech.glide.load.model.UriLoader}.
     */
    public RequestBuilder<ResourceType, TranscodeType> load(String string) {
        return loadGeneric(string);
    }

    /**
     * Returns a request builder to load the given {@link Uri}.
     *
     * <p>
     *     Note - this method caches data at Uris using only the Uri itself as the cache key. The data represented by
     *     Uris from some content providers may change without the Uri changing, which means using this method
     *     can lead to displaying stale data. Consider using
     *     {@link RequestOptions#signature(Key)} to mixin a signature
     *     you create based on the data at the given Uri that will invalidate the cache if that data changes.
     *     Alternatively, using {@link com.bumptech.glide.load.engine.DiskCacheStrategy#NONE} and/or
     *     {@link RequestOptions#skipMemoryCache(boolean)} may be appropriate.
     * </p>
     *
     * @see #load(Object)
     *
     * @param uri The Uri representing the image. Must be of a type handled by
     * {@link com.bumptech.glide.load.model.UriLoader}.
     */
    public RequestBuilder<ResourceType, TranscodeType> load(Uri uri) {
        return loadGeneric(uri);
    }

    /**
     * Returns a request builder to load the given {@link File}.
     *
     * <p>
     *     Note - this method caches data for Files using only the file path itself as the cache key. The data in the
     *     File can change so using this method can lead to displaying stale data. If you expect the data in the File to
     *     change, Consider using
     *     {@link RequestOptions#signature(com.bumptech.glide.load.Key)} to mixin a signature
     *     you create that identifies the data currently in the File that will invalidate the cache if that data
     *     changes. Alternatively, using {@link com.bumptech.glide.load.engine.DiskCacheStrategy#NONE} and/or
     *     {@link RequestOptions#skipMemoryCache(boolean)} may be appropriate.
     * </p>
     *
     * @see #load(Object)
     *
     * @param file The File containing the image
     */
    public RequestBuilder<ResourceType, TranscodeType> load(File file) {
        return loadGeneric(file);
    }

    /**
     * Returns a request builder to load the given resource id.
     * Returns a request builder that uses the {@link com.bumptech.glide.load.model.ModelLoaderFactory} currently
     * registered for {@link Integer} to load the image represented by the given {@link Integer} resource id. Defaults
     * to {@link com.bumptech.glide.load.model.stream.StreamResourceLoader.Factory} and
     * {@link com.bumptech.glide.load.model.stream.StreamResourceLoader} to load resource id models.
     *
     * <p>
     *     By default this method adds a version code based signature to the cache key used to cache this resource in
     *     Glide. This signature is sufficient to guarantee that end users will see the most up to date versions of
     *     your Drawables, but during development if you do not increment your version code before each install and
     *     you replace a Drawable with different data without changing the Drawable name, you may see inconsistent
     *     cached data. To get around this, consider using {@link com.bumptech.glide.load.engine.DiskCacheStrategy#NONE}
     *     via {@link RequestOptions#diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy)}
     *     during development, and re-enabling the default
     *     {@link com.bumptech.glide.load.engine.DiskCacheStrategy#RESULT} for release builds.
     * </p>
     *
     * @see #load(Integer)
     * @see com.bumptech.glide.signature.ApplicationVersionSignature
     */
    public RequestBuilder<ResourceType, TranscodeType> load(Integer resourceId) {
        return loadGeneric(resourceId)
                .apply(signatureOf(ApplicationVersionSignature.obtain(context)));
    }

    /**
     * Returns a request builder to load the given {@link URL}.
     *
     * @see #load(Object)
     *
     * @deprecated The {@link java.net.URL} class has
     * <a href="http://goo.gl/c4hHNu">a number of performance problems</a> and should generally be avoided when
     * possible. Prefer {@link #load(android.net.Uri)} or {@link #load(String)}.
     * @param url The URL representing the image.
     */
    @Deprecated
    public RequestBuilder<ResourceType, TranscodeType> load(URL url) {
        return loadGeneric(url);
    }

    /**
     * Returns a request to load the given byte array.
     *
     * <p>
     *     Note - by default loads for bytes are not cached in either the memory or the disk cache.
     * </p>
     *
     * @see #load(Object)
     *
     * @param model the data to load.
     */
    public RequestBuilder<ResourceType, TranscodeType> load(byte[] model) {
        return loadGeneric(model)
                .apply(signatureOf(new StringSignature(UUID.randomUUID()
                        .toString())).diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true /*skipMemoryCache*/));
    }

    /**
     * Returns a copy of this request builder with all of the options set so far on this builder.
     *
     * <p>
     *     This method returns a "deep" copy in that all non-immutable arguments are copied such that changes to one
     *     builder will not affect the other builder. However, in addition to immutable arguments, the current model
     *     is not copied copied so changes to the model will affect both builders.
     * </p>
     */
    @SuppressWarnings("unchecked")
    @Override
    public RequestBuilder<ResourceType, TranscodeType> clone() {
        try {
            RequestBuilder<ResourceType, TranscodeType> result =
                    (RequestBuilder<ResourceType, TranscodeType>) super.clone();
            result.requestOptions = result.requestOptions.clone();
            result.animationOptions = result.animationOptions.clone();
            result.transformationOptions = result.transformationOptions.clone();
            return result;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Set the target the resource will be loaded into.
     *
     * @see Glide#clear(com.bumptech.glide.request.target.Target)
     *
     * @param target The target to load the resource into.
     * @return The given target.
     */
    public <Y extends Target<TranscodeType>> Y into(Y target) {
        Util.assertMainThread();
        if (target == null) {
            throw new IllegalArgumentException("You must pass in a non null Target");
        }
        if (!isModelSet) {
            throw new IllegalArgumentException("You must first set a model (try #load())");
        }

        Request previous = target.getRequest();

        if (previous != null) {
            previous.clear();
            requestTracker.removeRequest(previous);
            previous.recycle();
        }

        Request request = buildRequest(target);
        target.setRequest(request);
        lifecycle.addListener(target);
        requestTracker.runRequest(request);

        return target;
    }

    /**
     * Sets the {@link ImageView} the resource will be loaded into, cancels any existing loads into the view, and frees
     * any resources Glide may have previously loaded into the view so they may be reused.
     *
     * @see Glide#clear(android.view.View)
     *
     * @param view The view to cancel previous loads for and load the new resource into.
     * @return The {@link com.bumptech.glide.request.target.Target} used to wrap the given {@link ImageView}.
     */
    public Target<TranscodeType> into(ImageView view) {
        Util.assertMainThread();
        if (view == null) {
            throw new IllegalArgumentException("You must pass in a non null View");
        }

        if (!transformationOptions.isTransformationSet() && view.getScaleType() != null) {
            switch (view.getScaleType()) {
                case CENTER_CROP:
                    transformationOptions.applyCenterCrop();
                    break;
                case FIT_CENTER:
                case FIT_START:
                case FIT_END:
                    transformationOptions.applyFitCenter();
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
     * @param width The desired width in pixels, or {@link Target#SIZE_ORIGINAL}. This will be overridden by
     *             {@link #override * (int, int)} if previously called.
     * @param height The desired height in pixels, or {@link Target#SIZE_ORIGINAL}. This will be overridden by
     *              {@link #override * (int, int)}} if previously called).
     *
     * @see Glide#clear(com.bumptech.glide.request.FutureTarget)
     *
     * @return An {@link com.bumptech.glide.request.FutureTarget} that can be used to obtain the
     *         resource in a blocking manner.
     */
    public FutureTarget<TranscodeType> into(int width, int height) {
        final RequestFutureTarget<TranscodeType> target =
                new RequestFutureTarget<TranscodeType>(context.getMainHandler(), width, height);

        // TODO: Currently all loads must be started on the main thread...
        context.getMainHandler().post(new Runnable() {
            @Override
            public void run() {
                if (!target.isCancelled()) {
                    into(target);
                }
            }
        });

        return target;
    }

    /**
     * Preloads the resource into the cache using the given width and height.
     *
     * <p>
     *     Pre-loading is useful for making sure that resources you are going to to want in the near future are
     *     available quickly.
     * </p>
     *
     *
     * @see com.bumptech.glide.ListPreloader
     *
     * @param width The desired width in pixels, or {@link Target#SIZE_ORIGINAL}. This will be overridden by
     *             {@link #override * (int, int)} if previously called.
     * @param height The desired height in pixels, or {@link Target#SIZE_ORIGINAL}. This will be overridden by
     *              {@link #override * (int, int)}} if previously called).
     * @return A {@link Target} that can be used to cancel the load via
     *        {@link Glide#clear(com.bumptech.glide.request.target.Target)}.
     */
    public Target<TranscodeType> preload(int width, int height) {
        final PreloadTarget<TranscodeType> target = PreloadTarget.obtain(width, height);
        return into(target);
    }

    /**
     * Preloads the resource into the cache using {@link Target#SIZE_ORIGINAL} as the target width and height.
     * Equivalent to calling {@link #preload(int, int)} with {@link Target#SIZE_ORIGINAL} as the width and height.
     *
     * @see #preload(int, int)
     *
     * @return A {@link Target} that can be used to cancel the load via
     *        {@link Glide#clear(com.bumptech.glide.request.target.Target)}.
     */
    public Target<TranscodeType> preload() {
        return preload(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
    }

    /**
     * Loads the original unmodified data into the cache and calls the given Target with the cache File.
     *
     * @param target The Target that will receive the cache File when the load completes
     * @param <Y> The type of Target.
     * @return The given Target.
     */
    public <Y extends Target<File>> Y downloadOnly(Y target) {
        // TODO: fixme.
//        return getDownloadOnlyRequest().downloadOnly(target);
        return target;
    }

    /**
     * Loads the original unmodified data into the cache and returns a {@link java.util.concurrent.Future} that can be
     * used to retrieve the cache File containing the data.
     *
     * @param width The width in pixels to use to fetch the data.
     * @param height The height in pixels to use to fetch the data.
     * @return A {@link java.util.concurrent.Future} that can be used to retrieve the cache File containing the data.
     */
    public FutureTarget<File> downloadOnly(int width, int height) {
        // TODO: fixme.
//        return getDownloadOnlyRequest().downloadOnly(width, height);
        return null;
    }

    private RequestBuilder<File, File> getDownloadOnlyRequest() {
        return new TranscodeRequest<File>(File.class, this)
                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .priority(Priority.LOW)
                        .skipMemoryCache(true));
    }

    private Priority getThumbnailPriority() {
        switch (requestOptions.getPriority()) {
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
        return buildRequestRecursive(target, null);
    }

    private Request buildRequestRecursive(Target<TranscodeType> target, ThumbnailRequestCoordinator parentCoordinator) {
        if (thumbnailBuilder != null) {
            // Recursive case: contains a potentially recursive thumbnail request builder.
            if (DEFAULT_ANIMATION_OPTIONS.equals(thumbnailBuilder.animationOptions)) {
                thumbnailBuilder.animationOptions = animationOptions;
            }

            if (!thumbnailBuilder.requestOptions.isPrioritySet()) {
                thumbnailBuilder.requestOptions.priority(getThumbnailPriority());
            }

            if (requestOptions.getOverrideWidth() > 0 && requestOptions.getOverrideHeight() > 0
                && thumbnailBuilder.requestOptions.getOverrideWidth() < 0
                && thumbnailBuilder.requestOptions.getOverrideHeight() < 0) {
              thumbnailBuilder.requestOptions.override(requestOptions.getOverrideWidth(), requestOptions
                      .getOverrideHeight());
            }

            ThumbnailRequestCoordinator coordinator = new ThumbnailRequestCoordinator(parentCoordinator);
            Request fullRequest = obtainRequest(target, requestOptions, coordinator);
            // Recursively generate thumbnail requests.
            Request thumbRequest = thumbnailBuilder.buildRequestRecursive(target, coordinator);
            coordinator.setRequests(fullRequest, thumbRequest);
            return coordinator;
        } else if (thumbSizeMultiplier != null) {
            // Base case: thumbnail multiplier generates a thumbnail request, but cannot recurse.
            ThumbnailRequestCoordinator coordinator = new ThumbnailRequestCoordinator(parentCoordinator);
            Request fullRequest = obtainRequest(target, requestOptions, coordinator);
            RequestOptions thumbnailOptions = new RequestOptions()
                    .apply(requestOptions)
                    .sizeMultiplier(thumbSizeMultiplier)
                    .priority(getThumbnailPriority());
            Request thumbnailRequest = obtainRequest(target, thumbnailOptions, coordinator);
            coordinator.setRequests(fullRequest, thumbnailRequest);
            return coordinator;
        } else {
            // Base case: no thumbnail.
            return obtainRequest(target, requestOptions, parentCoordinator);
        }
    }

    private Request obtainRequest(Target<TranscodeType> target, RequestOptions requestOptions,
            RequestCoordinator requestCoordinator) {
        RequestContext<ResourceType, TranscodeType> requestContext =
                new RequestContext<ResourceType, TranscodeType>(context, model, resourceClass,
                        transcodeClass, transformationOptions.getTransformation(), transcoder, requestOptions);

        return SingleRequest.obtain(requestContext, model, transcodeClass, requestOptions, target, requestListener,
                requestCoordinator, context.getEngine(), animationOptions.getAnimationFactory());
    }
}
