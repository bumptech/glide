package com.bumptech.glide;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageView;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.SkipCache;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.resource.bitmap.BitmapDecoder;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.provider.ChildLoadProvider;
import com.bumptech.glide.provider.LoadProvider;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestCoordinator;
import com.bumptech.glide.request.ThumbnailRequestCoordinator;
import com.bumptech.glide.request.bitmap.GenericRequest;
import com.bumptech.glide.request.bitmap.RequestListener;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.target.Target;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A generic class that can handle loading a bitmap either from an image or as a thumbnail from a video given
 * models loaders to translate a model into generic resources for either an image or a video and decoders that can
 * decode those resources into bitmaps.
 *
 * @param <ModelType> The type of model representing the image or video.
 * @param <DataType> The data type that the image {@link ModelLoader} will provide that can be decoded by the image
 *      {@link BitmapDecoder}.
 * @param <ResourceType> The type of the resource that will be loaded.
 */
public class GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> {
    private final Context context;
    private final List<Transformation<ResourceType>> transformations = new ArrayList<Transformation<ResourceType>>();
    private final ModelType model;
    private final ChildLoadProvider<ModelType, DataType, ResourceType, TranscodeType> loadProvider;
    private final Class<TranscodeType> transcodeClass;
    private final Glide glide;
    private int animationId;
    private Animation animation;
    private int placeholderId;
    private int errorId;
    private RequestListener<ModelType> requestListener;
    private Float thumbSizeMultiplier;
    private GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType>
            thumbnailRequestBuilder;
    private Float sizeMultiplier = 1f;
    private Drawable placeholderDrawable;
    private Drawable errorPlaceholder;
    private Priority priority = null;
    private boolean isCacheable = true;
    private ResourceEncoder<ResourceType> preSkipEncoder;

    GenericRequestBuilder(Context context, ModelType model,
            LoadProvider<ModelType, DataType, ResourceType, TranscodeType> loadProvider,
            Class<TranscodeType> transcodeClass, Glide glide) {
        this.transcodeClass = transcodeClass;
        this.glide = glide;
        this.loadProvider = loadProvider != null ?
                new ChildLoadProvider<ModelType, DataType, ResourceType, TranscodeType>(loadProvider) : null;
        preSkipEncoder = loadProvider != null ? loadProvider.getEncoder() : null;

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
     * Loads and displays the image retrieved by the given thumbnail request if it finishes before this request.
     * Best used for loading thumbnail images that are smaller and will be loaded more quickly than the fullsize
     * image. There are no guarantees about the order in which the requests will actually finish. However, if the
     * thumb request completes after the full request, the thumb image will never replace the full image.
     *
     * @see #thumbnail(float)
     *
     * <p>
     *     Note - Any options on the main request will not be passed on to the thumbnail request. For example, if
     *     you want an animation to occur when either the full image loads or the thumbnail loads, you need to call
     *     {@link #animate(int)} on both the thumb and the full request. For a simpler thumbnail option, see
     *     {@link #thumbnail(float)}.
     * </p>
     *
     * <p>
     *     Only the thumbnail call on the main request will be obeyed.
     * </p>
     *
     * @param thumbnailRequest The request to use to load the thumbnail.
     * @return This builder object.
     */
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> thumbnail(
            GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType>
                    thumbnailRequest) {
        this.thumbnailRequestBuilder = thumbnailRequest;

        return this;
    }

    /**
     * Loads an image in an identical manner to this request except with the dimensions of the target multiplied
     * by the given size multiplier. If the thumbnail load completes before the fullsize load, the thumbnail will
     * be shown. If the thumbnail load completes afer the fullsize load, the thumbnail will not be shown.
     *
     * <p>
     *     Note - The thumbnail image will be smaller than the size requested so the target (or {@link ImageView})
     *     must be able to scale the thumbnail appropriately. See {@link ImageView.ScaleType}.
     * </p>
     *
     * <p>
     *     Almost all options will be copied from the original load, including the {@link ModelLoader},
     *     {@link BitmapDecoder}, and {@link Transformation}s. However, {@link #placeholder(int)} and
     *     {@link #error(int)}, and {@link #listener(RequestListener)} will only be used on the fullsize load and
     *     will not be copied for the thumbnail load.
     * </p>
     *
     * <p>
     *     Only the thumbnail call on the main request will be obeyed.
     * </p>
     *
     * @param sizeMultiplier The multiplier to apply to the {@link Target}'s dimensions when loading the thumbnail.
     * @return This builder object.
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
     * Applies a multiplier to the {@link Target}'s size before loading the image. Useful for loading thumbnails
     * or trying to avoid loading huge bitmaps on devices with overly dense screens.
     *
     * @param sizeMultiplier The multiplier to apply to the {@link Target}'s dimensions when loading the image.
     * @return This builder object.
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
     * Loads the resource from the given data type using the given {@link BitmapDecoder}.
     *
     * <p>
     *     Will be ignored if the data represented by the given model is not a video.
     * </p>
     *
     * @param decoder The {@link BitmapDecoder} to use to decode the video resource.
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

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> cacheDecoder(
            ResourceDecoder <InputStream, ResourceType> cacheDecoder) {
        // loadProvider will be null if model is null, in which case we're not going to load anything so it's ok to
        // ignore the decoder.
        if (loadProvider != null) {
            loadProvider.setCacheDecoder(cacheDecoder);
        }

        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> encoder(
            ResourceEncoder<ResourceType> encoder) {
        // loadProvider will be null if model is null, in which case we're not going to load anything so it's ok to
        // ignore the encoder.
        if (loadProvider != null) {
            loadProvider.setEncoder(encoder);
            preSkipEncoder = encoder;
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
     * Transform images with the given {@link Transformation}. Appends this transformation onto any existing
     * transformations
     *
     * @param transformation the transformation to apply.
     * @return This RequestBuilder
     */
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> transform(
            Transformation<ResourceType> transformation) {
        transformations.add(transformation);

        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> transcoder(
            ResourceTranscoder<ResourceType, TranscodeType> transcoder) {
        if (loadProvider != null) {
            loadProvider.setTranscoder(transcoder);
        }

        return this;
    }

    /**
     * Sets an animation to run on the wrapped target when an image load finishes. Will only be run if the image
     * was loaded asynchronously (ie was not in the memory cache)
     *
     * @param animationId The resource id of the animation to run
     * @return This RequestBuilder
     */
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> animate(
            int animationId) {
        this.animationId = animationId;

        return this;
    }

    /**
     * Sets an animation to run on the wrapped target when an image load finishes. Will only be run if the image
     * was loaded asynchronously (ie was not in the memory cache)
     *
     * @param animation The animation to run
     * @return This RequestBuilder
     */
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> animate(
            Animation animation) {
        this.animation = animation;

        return this;
    }

    /**
     * Sets a resource to display while an image is loading
     *
     * @param resourceId The id of the resource to use as a placeholder
     * @return This RequestBuilder
     */
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> placeholder(
            int resourceId) {
        this.placeholderId = resourceId;

        return this;
    }

    /**
     * Sets a drawable to display while an image is loading.
     *
     * @param drawable The drawable to display as a placeholder.
     * @return This RequestBuilder.
     */
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> placeholder(
            Drawable drawable) {
        this.placeholderDrawable = drawable;

        return this;
    }

    /**
     * Sets a resource to display if a load fails
     *
     * @param resourceId The id of the resource to use as a placeholder
     * @return This request
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
     * @return This RequestBuilder.
     */
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> error(
            Drawable drawable) {
        this.errorPlaceholder = drawable;

        return this;
    }

    /**
     * Sets a RequestBuilder listener to monitor the image load. It's best to create a single instance of an
     * exception handler per type of request (usually activity/fragment) rather than pass one in per request to
     * avoid some redundant object allocation.
     *
     * @param requestListener The request listener to use.
     * @return This RequestBuilder.
     */
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> listener(
            RequestListener<ModelType> requestListener) {
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
     * @return This RequestBuilder.
     */
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> skipMemoryCache(boolean skip) {
        this.isCacheable = !skip;

        return this;
    }

    /**
     * Allows the loaded resource to skip the disk cache.
     *
     * <p>
     *     Note - this is not a guarantee. If a request is already pending for this resource and that request is not
     *     also skipping the disk cache, the resource will be cached on disk.
     * </p>
     *
     * @param skip True to allow the resource to skip the disk cache.
     * @return This RequestBuilder.
     */
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> skipDiskCache(boolean skip) {
        if (skip) {
            if (loadProvider != null) {
                preSkipEncoder = loadProvider.getEncoder();
            }
            final SkipCache<ResourceType> skipCache = SkipCache.get();
            return encoder(skipCache);
        } else {
            return encoder(preSkipEncoder);
        }
    }

    /**
     * Allows the resource to skip both the memory and the disk cache.
     *
     * @see #skipDiskCache(boolean)
     * @see #skipMemoryCache(boolean)
     *
     * @param skip True to allow the resource to skip both the memory and the disk cache.
     * @return This RequestBuilder.
     */
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> skipCache(boolean skip) {
        skipMemoryCache(skip);
        skipDiskCache(skip);

        return this;
    }

    /**
     * Set the target the image will be loaded into.
     *
     * @param target The target to load te image for
     * @return The given target.
     */
    public <Y extends Target<TranscodeType>> Y into(Y target) {
        Request previous = target.getRequest();
        if (previous != null) {
            previous.clear();
        }

        Request request = buildRequest(target);
        target.setRequest(request);
        if (request != null) {
            request.run();
        }
        return target;
    }

    /**
     * Sets the {@link ImageView} the image will be loaded into, cancels any existing loads into the view, and frees
     * any resources Glide has loaded into the view so they may be reused.
     *
     * @see Glide#clear(View)
     *
     * @param view The view to cancel previous loads for and load the new image into.
     * @return The {@link BitmapImageViewTarget} used to wrap the given {@link ImageView}.
     */
    public Target<TranscodeType> into(ImageView view) {
        return into(glide.buildImageViewTarget(view, transcodeClass));
    }

    private Request buildRequest(Target<TranscodeType> target) {
        final Request result;

        if (priority == null) {
            priority = Priority.NORMAL;
        }

        if (thumbnailRequestBuilder != null) {
            ThumbnailRequestCoordinator requestCoordinator = new ThumbnailRequestCoordinator();
            Request fullRequest = buildRequest(target, sizeMultiplier, priority, requestCoordinator);

            if (thumbnailRequestBuilder.animationId <= 0) {
                thumbnailRequestBuilder.animationId = animationId;
            }

            if (thumbnailRequestBuilder.animation == null) {
                thumbnailRequestBuilder.animation = animation;
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
        return new GenericRequest<ModelType, Z, ResourceType, TranscodeType>(loadProvider, model, context, priority,
                target, sizeMultiplier, placeholderDrawable, placeholderId, errorPlaceholder, errorId, requestListener,
                animationId, animation, requestCoordinator, glide.getEngine(), getFinalTransformation(),
                transcodeClass, isCacheable);
    }

    @SuppressWarnings("unchecked")
    private Transformation<ResourceType> getFinalTransformation() {
        switch (transformations.size()) {
            case 0:
                return Transformation.NONE;
            case 1:
                return transformations.get(0);
            default:
                return new MultiTransformation<ResourceType>(transformations);
        }
    }
}
