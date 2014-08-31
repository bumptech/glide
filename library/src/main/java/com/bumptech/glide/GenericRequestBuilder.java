package com.bumptech.glide;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.animation.Animation;
import android.widget.ImageView;

import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.UnitTransformation;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.manager.Lifecycle;
import com.bumptech.glide.manager.RequestTracker;
import com.bumptech.glide.provider.ChildLoadProvider;
import com.bumptech.glide.provider.LoadProvider;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.GenericRequest;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestCoordinator;
import com.bumptech.glide.request.RequestFutureTarget;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.ThumbnailRequestCoordinator;
import com.bumptech.glide.request.animation.GlideAnimationFactory;
import com.bumptech.glide.request.animation.NoAnimation;
import com.bumptech.glide.request.animation.ViewAnimation;
import com.bumptech.glide.request.animation.ViewPropertyAnimation;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.util.Util;

import java.io.File;

/**
 * A generic class that can handle setting options and staring loads for generic resource types.
 *
 * @param <ModelType> The type of model representing the resource.
 * @param <DataType> The data type that the resource {@link com.bumptech.glide.load.model.ModelLoader} will provide that
 *                  can be decoded by the {@link com.bumptech.glide.load.ResourceDecoder}.
 * @param <ResourceType> The type of the resource that will be loaded.
 * @param <TranscodeType> The type of resource the decoded resource will be transcoded to.
 */
public class GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> {
    private final Context context;
    private final ModelType model;
    private final ChildLoadProvider<ModelType, DataType, ResourceType, TranscodeType> loadProvider;
    private final Class<TranscodeType> transcodeClass;
    private final Glide glide;
    private final RequestTracker requestTracker;
    private Lifecycle lifecycle;
    private int placeholderId;
    private int errorId;
    private RequestListener<ModelType, TranscodeType> requestListener;
    private Float thumbSizeMultiplier;
    private GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType>
            thumbnailRequestBuilder;
    private Float sizeMultiplier = 1f;
    private Drawable placeholderDrawable;
    private Drawable errorPlaceholder;
    private Priority priority = null;
    private boolean isCacheable = true;
    private GlideAnimationFactory<TranscodeType> animationFactory = NoAnimation.getFactory();
    private int overrideHeight = -1;
    private int overrideWidth = -1;
    private DiskCacheStrategy diskCacheStrategy = DiskCacheStrategy.RESULT;
    private Transformation<ResourceType> transformation = UnitTransformation.get();
    private boolean isTransformationSet;

    GenericRequestBuilder(Context context, ModelType model,
            LoadProvider<ModelType, DataType, ResourceType, TranscodeType> loadProvider,
            Class<TranscodeType> transcodeClass, Glide glide, RequestTracker requestTracker, Lifecycle lifecycle) {
        this.transcodeClass = transcodeClass;
        this.glide = glide;
        this.requestTracker = requestTracker;
        this.lifecycle = lifecycle;
        this.loadProvider = loadProvider != null
                ? new ChildLoadProvider<ModelType, DataType, ResourceType, TranscodeType>(loadProvider) : null;

        if (context == null) {
            throw new NullPointerException("Context can't be null");
        }
        if (model != null && loadProvider == null) {
            throw new NullPointerException("LoadProvider must not be null");
        }
        this.context = context;
        this.model = model;
    }

    /**
     * Loads and displays the resource retrieved by the given thumbnail request if it finishes before this request.
     * Best used for loading thumbnail resources that are smaller and will be loaded more quickly than the fullsize
     * resource. There are no guarantees about the order in which the requests will actually finish. However, if the
     * thumb request completes after the full request, the thumb resource will never replace the full resource.
     *
     * @see #thumbnail(float)
     *
     * <p>
     *     Note - Any options on the main request will not be passed on to the thumbnail request. For example, if
     *     you want an animation to occur when either the full resource loads or the thumbnail loads, you need to call
     *     {@link #animate(int)} on both the thumb and the full request. For a simpler thumbnail option, see
     *     {@link #thumbnail(float)}.
     * </p>
     *
     * <p>
     *     Only the thumbnail call on the main request will be obeyed.
     * </p>
     *
     * @param thumbnailRequest The request to use to load the thumbnail.
     * @return This request builder.
     */
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> thumbnail(
            GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType>
                    thumbnailRequest) {
        this.thumbnailRequestBuilder = thumbnailRequest;

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
     *     {@link Transformation}s. However, {@link #placeholder(int)} and {@link #error(int)},
     *     and {@link #listener(RequestListener)} will only be used on the fullsize load and will not be copied for
     *     the thumbnail load.
     * </p>
     *
     * <p>
     *     Only the thumbnail call on the main request will be obeyed.
     * </p>
     *
     * @param sizeMultiplier The multiplier to apply to the {@link Target}'s dimensions when loading the thumbnail.
     * @return This request builder.
     */
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> thumbnail(
            float sizeMultiplier) {
        if (sizeMultiplier < 0f || sizeMultiplier > 1f) {
            throw new IllegalArgumentException("sizeMultiplier must be between 0 and 1");
        }
        this.thumbSizeMultiplier = sizeMultiplier;

        return this;
    }

    /**
     * Applies a multiplier to the {@link Target}'s size before loading the resource. Useful for loading thumbnails
     * or trying to avoid loading huge resources (particularly {@link android.graphics.Bitmap}s on devices with overly
     * dense screens.
     *
     * @param sizeMultiplier The multiplier to apply to the {@link Target}'s dimensions when loading the resource.
     * @return This request builder.
     */
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> sizeMultiplier(
            float sizeMultiplier) {
        if (sizeMultiplier < 0f || sizeMultiplier > 1f) {
            throw new IllegalArgumentException("sizeMultiplier must be between 0 and 1");
        }
        this.sizeMultiplier = sizeMultiplier;

        return this;
    }

    /**
     * Sets the {@link com.bumptech.glide.load.ResourceDecoder} to use to load the resource from the original data.
     * By default, this decoder will only be used if the final transformed resource is not in the disk cache.
     *
     * @see #cacheDecoder(com.bumptech.glide.load.ResourceDecoder)
     * @see com.bumptech.glide.load.engine.DiskCacheStrategy
     *
     * @param decoder The {@link com.bumptech.glide.load.ResourceDecoder} to use to decode the resource.
     * @return This request builder.
     */
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> decoder(
            ResourceDecoder<DataType, ResourceType> decoder) {
        // loadProvider will be null if model is null, in which case we're not going to load anything so it's ok to
        // ignore the decoder.
        if (loadProvider != null) {
            loadProvider.setSourceDecoder(decoder);
        }

        return this;
    }

    /**
     * Sets the {@link com.bumptech.glide.load.ResourceDecoder} to use to load the resource from the disk cache. By
     * default, this decoder will only be used if the final transformed resource is already in the disk cache.
     *
     * @see #decoder(com.bumptech.glide.load.ResourceDecoder)
     * @see com.bumptech.glide.load.engine.DiskCacheStrategy
     *
     * @param cacheDecoder The decoder to use.
     * @return This request builder.
     */
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> cacheDecoder(
            ResourceDecoder<File, ResourceType> cacheDecoder) {
        // loadProvider will be null if model is null, in which case we're not going to load anything so it's ok to
        // ignore the decoder.
        if (loadProvider != null) {
            loadProvider.setCacheDecoder(cacheDecoder);
        }

        return this;
    }

    /**
     * Sets the source encoder to use to encode the data retrieved by this request directly into cache. The returned
     * resource will then be decoded from the cached data.
     *
     * @see com.bumptech.glide.load.engine.DiskCacheStrategy
     *
     * @param sourceEncoder The encoder to use.
     * @return This request builder.
     */
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> sourceEncoder(
            Encoder<DataType> sourceEncoder) {
        if (loadProvider != null) {
            loadProvider.setSourceEncoder(sourceEncoder);
        }

        return this;
    }

    /**
     * Sets the {@link com.bumptech.glide.load.engine.DiskCacheStrategy} to use for this load. Defaults to
     * {@link com.bumptech.glide.load.engine.DiskCacheStrategy#RESULT}.
     *
     * <p>
     *     For most applications {@link com.bumptech.glide.load.engine.DiskCacheStrategy#RESULT} is ideal.
     *     Applications that use the same resource multiple times in multiple sizes and are willing to trade off some
     *     speed and disk space in return for lower bandwidth usage may want to consider using
     *     {@link com.bumptech.glide.load.engine.DiskCacheStrategy#SOURCE} or
     *     {@link com.bumptech.glide.load.engine.DiskCacheStrategy#RESULT}. Any download only operations should
     *     typically use {@link com.bumptech.glide.load.engine.DiskCacheStrategy#SOURCE}.
     * </p>
     *
     * @param strategy The strategy to use.
     * @return This request builder.
     */
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType>  diskCacheStrategy(
            DiskCacheStrategy strategy) {
        this.diskCacheStrategy = strategy;

        return this;
    }

    /**
     * Sets the {@link com.bumptech.glide.load.Encoder} to use to encode the original data directly to cache. Will only
     * be used if the original data is not already in cache and if the
     * {@link com.bumptech.glide.load.engine.DiskCacheStrategy} is set to
     * {@link com.bumptech.glide.load.engine.DiskCacheStrategy#SOURCE} or
     * {@link com.bumptech.glide.load.engine.DiskCacheStrategy#ALL}.
     *
     * @see #sourceEncoder(com.bumptech.glide.load.Encoder)
     * @see com.bumptech.glide.load.engine.DiskCacheStrategy
     *
     * @param encoder The encoder to use.
     * @return This request builder.
     */
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> encoder(
            ResourceEncoder<ResourceType> encoder) {
        // loadProvider will be null if model is null, in which case we're not going to load anything so it's ok to
        // ignore the encoder.
        if (loadProvider != null) {
            loadProvider.setEncoder(encoder);
        }

        return this;
    }

    /**
     * Sets the priority for this load.
     *
     * @param priority A priority.
     * @return This request builder.
     */
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> priority(
            Priority priority) {
        this.priority = priority;

        return this;
    }

    /**
     * Transform resources with the given {@link Transformation}s. Replaces any existing transformation or
     * transformations.
     *
     * @param transformations the transformations to apply in order.
     * @return This request builder.
     */
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> transform(
            Transformation<ResourceType>... transformations) {
        isTransformationSet = true;
        if (transformations.length == 1) {
            transformation = transformations[0];
        } else {
            transformation = new MultiTransformation<ResourceType>(transformations);
        }

        return this;
    }

    /**
     * Removes the current {@link com.bumptech.glide.load.Transformation}.
     *
     * @return This request builder.
     */
    @SuppressWarnings("unchecked")
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> dontTransform() {
        Transformation<ResourceType> transformation = UnitTransformation.get();
        return transform(transformation);
    }

    /**
     * Sets the {@link com.bumptech.glide.load.resource.transcode.ResourceTranscoder} to use for this load.
     *
     * @see com.bumptech.glide.load.resource.transcode.UnitTranscoder
     * @see com.bumptech.glide.load.resource.transcode.GlideBitmapDrawableTranscoder
     * @see com.bumptech.glide.load.resource.transcode.GifBitmapWrapperDrawableTranscoder
     *
     * @param transcoder The transcoder to use.
     * @return This request builder.
     */
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> transcoder(
            ResourceTranscoder<ResourceType, TranscodeType> transcoder) {
        if (loadProvider != null) {
            loadProvider.setTranscoder(transcoder);
        }

        return this;
    }

    /**
     * Removes any existing animation set on the builder. Will be overridden by subsequent calls that set an animation.
     * @return This request builder.
     */
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> dontAnimate() {
        GlideAnimationFactory<TranscodeType> animation = NoAnimation.getFactory();
        return animate(animation);
    }

    /**
     * Sets an animation to run on the wrapped target when an resource load finishes. Will only be run if the resource
     * was loaded asynchronously (ie was not in the memory cache)
     *
     * @param animationId The resource id of the animation to run
     * @return This request builder.
     */
    // This is safe because the view animation doesn't care about the resource type it receives.
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> animate(
            int animationId) {
        return animate(new ViewAnimation.ViewAnimationFactory<TranscodeType>(context, animationId));
    }

    /**
     * Sets an animation to run on the wrapped target when a resource load finishes. Will only be run if the resource
     * was loaded asynchronously (ie was not in the memory cache)
     *
     * @param animation The animation to run
     * @return This request builder.
     */
    // This is safe because the view animation doesn't care about the resource type it receives.
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> animate(
            Animation animation) {
        return animate(new ViewAnimation.ViewAnimationFactory<TranscodeType>(animation));
    }

    /**
     * Sets an animator to run a {@link android.view.ViewPropertyAnimator} on a view that the target may be wrapping
     * when a resource load finishes. Will only be run if the load was loaded asynchronously (ie was not in the
     * memory cache).
     *
     * @param animator The {@link com.bumptech.glide.request.animation.ViewPropertyAnimation.Animator} to run.
     * @return This request builder.
     */
    // This is safe because the view property animation doesn't care about the resource type it receives.
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> animate(
            ViewPropertyAnimation.Animator animator) {
        return animate(new ViewPropertyAnimation.ViewPropertyAnimationFactory<TranscodeType>(animator));
    }

    GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> animate(
            GlideAnimationFactory<TranscodeType> animationFactory) {
        if (animationFactory == null) {
            throw new NullPointerException("Animation factory must not be null!");
        }
        this.animationFactory = animationFactory;

        return this;
    }

    /**
     * Sets an Android resource id for a {@link android.graphics.drawable.Drawable} resourceto display while a resource
     * is loading.
     *
     * @param resourceId The id of the resource to use as a placeholder
     * @return This request builder.
     */
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> placeholder(
            int resourceId) {
        this.placeholderId = resourceId;

        return this;
    }

    /**
     * Sets an {@link android.graphics.drawable.Drawable} to display while a resource is loading.
     *
     * @param drawable The drawable to display as a placeholder.
     * @return This request builder.
     */
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> placeholder(
            Drawable drawable) {
        this.placeholderDrawable = drawable;

        return this;
    }

    /**
     * Sets a resource to display if a load fails.
     *
     * @param resourceId The id of the resource to use as a placeholder.
     * @return This request builder.
     */
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> error(
            int resourceId) {
        this.errorId = resourceId;

        return this;
    }

    /**
     * Sets a {@link Drawable} to display if a load fails.
     *
     * @param drawable The drawable to display.
     * @return This request builder.
     */
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> error(
            Drawable drawable) {
        this.errorPlaceholder = drawable;

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
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> listener(
            RequestListener<ModelType, TranscodeType> requestListener) {
        this.requestListener = requestListener;

        return this;
    }

    /**
     * Allows the loaded resource to skip the memory cache.
     *
     * <p>
     *     Note - this is not a guarantee. If a request is already pending for this resource and that request is not
     *     also skipping the memory cache, the resource will be cached in memory.
     * </p>
     *
     * @param skip True to allow the resource to skip the memory cache.
     * @return This request builder.
     */
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> skipMemoryCache(boolean skip) {
        this.isCacheable = !skip;

        return this;
    }

    /**
     * Overrides the {@link Target}'s width and height with the given values. This is useful almost exclusively for
     * thumbnails, and should only be used when you both need a very specific sized image and when it is impossible or
     * impractical to return that size from {@link Target#getSize(com.bumptech.glide.request.target.SizeReadyCallback)}.
     *
     * @param width The width to use to load the resource.
     * @param height The height to use to load the resource.
     * @return This request builder.
     */
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> override(int width, int height) {
        if (width <= 0) {
            throw new IllegalArgumentException("Width must be > 0");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("Height must be > 0");
        }
        this.overrideWidth = width;
        this.overrideHeight = height;

        return this;
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

        if (!isTransformationSet && view.getScaleType() != null) {
            switch (view.getScaleType()) {
                case CENTER_CROP:
                    applyCenterCrop();
                    break;
                case FIT_CENTER:
                case FIT_START:
                case FIT_END:
                    applyFitCenter();
                    break;
                //$CASES-OMITTED$
                default:
                    // silently ignore
                    break;
            }
        }

        return into(glide.buildImageViewTarget(view, transcodeClass));
    }

    /**
     * Returns a future that can be used to do a blocking get on a background thread.
     *
     * @param width The desired width (note this will be overriden by {@link #override(int, int)} if
     *              previously called.
     * @param height The desired height (note this will be overriden by {@link #override(int, int)}}
     *               if previously called.
     *
     * @see Glide#clear(com.bumptech.glide.request.FutureTarget)
     *
     * @return An {@link com.bumptech.glide.request.FutureTarget} that can be used to obtain the
     *         resource in a blocking manner.
     */
    public FutureTarget<TranscodeType> into(int width, int height) {
        final RequestFutureTarget<ModelType, TranscodeType> target =
                new RequestFutureTarget<ModelType, TranscodeType>(glide.getMainHandler(), width, height);

        // TODO: Currently all loads must be started on the main thread...
        glide.getMainHandler().post(new Runnable() {
            @Override
            public void run() {
                if (!target.isCancelled()) {
                    into(target);
                }
            }
        });

        return target;
    }

    void applyCenterCrop() {
        // To be implemented by subclasses when possible.
    }

    void applyFitCenter() {
        // To be implemented by subclasses when possible.
    }

    private Request buildRequest(Target<TranscodeType> target) {
        final Request result;

        if (priority == null) {
            priority = Priority.NORMAL;
        }

        if (thumbnailRequestBuilder != null) {
            ThumbnailRequestCoordinator requestCoordinator = new ThumbnailRequestCoordinator();
            Request fullRequest = buildRequest(target, sizeMultiplier, priority, requestCoordinator);

            if (thumbnailRequestBuilder.animationFactory.equals(NoAnimation.getFactory())) {
                thumbnailRequestBuilder.animationFactory = animationFactory;
            }

            if (thumbnailRequestBuilder.requestListener == null && requestListener != null) {
                thumbnailRequestBuilder.requestListener = requestListener;
            }

            if (thumbnailRequestBuilder.priority == null) {
                thumbnailRequestBuilder.priority = getThumbnailPriority();
            }

            Request thumbnailRequest = thumbnailRequestBuilder.buildRequest(target,
                    thumbnailRequestBuilder.sizeMultiplier, thumbnailRequestBuilder.priority, requestCoordinator);

            requestCoordinator.setRequests(fullRequest, thumbnailRequest);
            result = requestCoordinator;
        } else if (thumbSizeMultiplier != null) {
            ThumbnailRequestCoordinator requestCoordinator = new ThumbnailRequestCoordinator();
            Request fullRequest = buildRequest(target, sizeMultiplier, priority, requestCoordinator);
            Request thumbnailRequest = buildRequest(target, thumbSizeMultiplier, getThumbnailPriority(),
                    requestCoordinator);
            requestCoordinator.setRequests(fullRequest, thumbnailRequest);
            result = requestCoordinator;
        } else {
            result = buildRequest(target, sizeMultiplier, priority, null);
        }
        return result;
    }

    private Priority getThumbnailPriority() {
        final Priority result;
        if (priority == Priority.LOW) {
            result = Priority.NORMAL;
        } else if (priority == Priority.NORMAL) {
            result = Priority.HIGH;
        } else {
            result = Priority.IMMEDIATE;
        }
        return result;
    }

    private Request buildRequest(Target<TranscodeType> target, float sizeMultiplier, Priority priority,
            RequestCoordinator requestCoordinator) {
        if (model == null) {
            return buildRequestForDataType(target, loadProvider, sizeMultiplier, priority, null);
        } else {
            return buildRequestForDataType(target, loadProvider, sizeMultiplier, priority, requestCoordinator);
        }
    }

    private <Z> Request buildRequestForDataType(Target<TranscodeType> target,
            LoadProvider<ModelType, Z, ResourceType, TranscodeType> loadProvider, float sizeMultiplier,
            Priority priority, RequestCoordinator requestCoordinator) {
        return GenericRequest.obtain(
                loadProvider,
                model,
                context,
                priority,
                target,
                sizeMultiplier,
                placeholderDrawable,
                placeholderId,
                errorPlaceholder,
                errorId,
                requestListener,
                requestCoordinator,
                glide.getEngine(),
                transformation,
                transcodeClass,
                isCacheable,
                animationFactory,
                overrideWidth,
                overrideHeight,
                diskCacheStrategy);
    }
}
