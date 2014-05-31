package com.bumptech.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.animation.Animation;
import android.widget.ImageView;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.data.bitmap.BitmapDecoder;
import com.bumptech.glide.load.data.bitmap.Downsampler;
import com.bumptech.glide.load.data.bitmap.VideoBitmapDecoder;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.provider.ChildLoadProvider;
import com.bumptech.glide.provider.LoadProvider;
import com.bumptech.glide.request.MultiTypeRequestCoordinator;
import com.bumptech.glide.request.RequestCoordinator;
import com.bumptech.glide.request.ThumbnailRequestCoordinator;
import com.bumptech.glide.request.bitmap.GenericRequest;
import com.bumptech.glide.request.bitmap.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.ArrayList;
import java.util.List;

/**
 * A generic class that can handle loading a bitmap either from an image or as a thumbnail from a video given
 * models loaders to translate a model into generic resources for either an image or a video and decoders that can
 * decode those resources into bitmaps.
 *
 * @param <ModelType> The type of model representing the image or video.
 * @param <ImageDataType> The data type that the image {@link ModelLoader} will provide that can be decoded by the image
 *      {@link BitmapDecoder}.
 * @param <VideoDataType> The data type that the video {@link ModelLoader} will provide that can be decoded by the video
 *      {@link BitmapDecoder}.
 * @param <ResourceType> The type of the resource that will be loaded.
 */
public class GenericRequestBuilder<ModelType, ImageDataType, VideoDataType, ResourceType> {
    protected final Context context;
    private final List<Transformation<ResourceType>> transformations = new ArrayList<Transformation<ResourceType>>();
    private final ModelType model;
    private final ChildLoadProvider<ModelType, ImageDataType, ResourceType> imageLoadProvider;
    private final ChildLoadProvider<ModelType, VideoDataType, ResourceType> videoLoadProvider;
    private int animationId;
    private Animation animation;
    private int placeholderId;
    private int errorId;
    private RequestListener<ModelType> requestListener;
    private Float thumbSizeMultiplier;
    private GenericRequestBuilder<ModelType, ImageDataType, VideoDataType, ResourceType> thumbnailRequestBuilder;
    private Float sizeMultiplier = 1f;
    private Drawable placeholderDrawable;
    private Drawable errorPlaceholder;
    private Priority priority = null;

    public GenericRequestBuilder(Context context, ModelType model,
            LoadProvider<ModelType, ImageDataType, ResourceType> imageLoadProvider,
            LoadProvider<ModelType, VideoDataType, ResourceType> videoLoadProvider) {
        this.imageLoadProvider = imageLoadProvider != null ?
                new ChildLoadProvider<ModelType, ImageDataType, ResourceType>(imageLoadProvider) : null;
        this.videoLoadProvider = videoLoadProvider != null ?
                new ChildLoadProvider<ModelType, VideoDataType, ResourceType>(videoLoadProvider) : null;

        if (context == null) {
            throw new NullPointerException("Context can't be null");
        }
        if (model != null && imageLoadProvider == null && videoLoadProvider == null) {
            throw new NullPointerException("At least one of imageLoadProvider and videoLoadProvider must not be null");
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
    public GenericRequestBuilder<ModelType, ImageDataType, VideoDataType, ResourceType> thumbnail(
            GenericRequestBuilder<ModelType, ImageDataType, VideoDataType, ResourceType> thumbnailRequest) {
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
    public GenericRequestBuilder<ModelType, ImageDataType, VideoDataType, ResourceType> thumbnail(float sizeMultiplier) {
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
    public GenericRequestBuilder<ModelType, ImageDataType, VideoDataType, ResourceType> sizeMultiplier(
            float sizeMultiplier) {
        if (sizeMultiplier < 0f || sizeMultiplier > 1f) {
            throw new IllegalArgumentException("sizeMultiplier must be between 0 and 1");
        }
        this.sizeMultiplier = sizeMultiplier;

        return this;
    }

    /**
     * Loads the image from the given resource type into an {@link Bitmap} using the given {@link BitmapDecoder}.
     *
     * <p>
     *     Will be ignored if the data represented by the given model is not an image.
     * </p>
     *
     * @see Downsampler
     *
     * @param decoder The {@link BitmapDecoder} to use to decode the image resource.
     * @return This RequestBuilder.
     */
    public GenericRequestBuilder<ModelType, ImageDataType, VideoDataType, ResourceType> imageDecoder(
            ResourceDecoder<ImageDataType, ResourceType> decoder) {
        imageLoadProvider.setSourceDecoder(decoder);

        return this;
    }

    /**
     * Loads the video from the given resource type into an {@link Bitmap} using the given {@link BitmapDecoder}.
     *
     * <p>
     *     Will be ignored if the data represented by the given model is not a video.
     * </p>
     *
     * @see VideoBitmapDecoder
     *
     * @param decoder The {@link BitmapDecoder} to use to decode the video resource.
     * @return This request builder.
     */
    public GenericRequestBuilder<ModelType, ImageDataType, VideoDataType, ResourceType> videoDecoder(
            ResourceDecoder<VideoDataType, ResourceType> decoder) {
        videoLoadProvider.setSourceDecoder(decoder);

        return this;
    }

    /**
     * Sets the priority for this load.
     *
     * @param priority A priority.
     * @return This request builder.
     */
    public GenericRequestBuilder<ModelType, ImageDataType, VideoDataType, ResourceType> priority(Priority priority) {
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
    public GenericRequestBuilder<ModelType, ImageDataType, VideoDataType, ResourceType> transform(
            Transformation<ResourceType> transformation) {
        transformations.add(transformation);

        return this;
    }

    /**
     * Sets an animation to run on the wrapped target when an image load finishes. Will only be run if the image
     * was loaded asynchronously (ie was not in the memory cache)
     *
     * @param animationId The resource id of the animation to run
     * @return This RequestBuilder
     */
    public GenericRequestBuilder<ModelType, ImageDataType, VideoDataType, ResourceType> animate(int animationId) {
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
    public GenericRequestBuilder<ModelType, ImageDataType, VideoDataType, ResourceType> animate(Animation animation) {
        this.animation = animation;

        return this;
    }

    /**
     * Sets a resource to display while an image is loading
     *
     * @param resourceId The id of the resource to use as a placeholder
     * @return This RequestBuilder
     */
    public GenericRequestBuilder<ModelType, ImageDataType, VideoDataType, ResourceType> placeholder(int resourceId) {
        this.placeholderId = resourceId;

        return this;
    }

    /**
     * Sets a drawable to display while an image is loading.
     *
     * @param drawable The drawable to display as a placeholder.
     * @return This RequestBuilder.
     */
    public GenericRequestBuilder<ModelType, ImageDataType, VideoDataType, ResourceType> placeholder(Drawable drawable) {
        this.placeholderDrawable = drawable;

        return this;
    }

    /**
     * Sets a resource to display if a load fails
     *
     * @param resourceId The id of the resource to use as a placeholder
     * @return This request
     */
    public GenericRequestBuilder<ModelType, ImageDataType, VideoDataType, ResourceType> error(int resourceId) {
        this.errorId = resourceId;

        return this;
    }

    /**
     * Sets a {@link Drawable} to display if a load fails.
     *
     * @param drawable The drawable to display.
     * @return This RequestBuilder.
     */
    public GenericRequestBuilder<ModelType, ImageDataType, VideoDataType, ResourceType> error(Drawable drawable) {
        this.errorPlaceholder = drawable;

        return this;
    }

    /**
     * Sets a RequestBuilder listener to monitor the image load. It's best to create a single instance of an
     * exception handler per type of request (usually activity/fragment) rather than pass one in per request to
     * avoid some redundant object allocation.
     *
     * @param requestListener The request listener to use
     * @return This request
     */
    public GenericRequestBuilder<ModelType, ImageDataType, VideoDataType, ResourceType> listener(
            RequestListener<ModelType> requestListener) {
        this.requestListener = requestListener;

        return this;
    }

    /**
     * Set the target the image will be loaded into.
     *
     * @param target The target to load te image for
     * @return The given target.
     */
    public <Y extends Target<ResourceType>> Y into(Y target) {
        com.bumptech.glide.request.Request previous = target.getRequest();
        if (previous != null) {
            previous.clear();
        }

        com.bumptech.glide.request.Request request = buildRequest(target);
        target.setRequest(request);
        if (request != null) {
            request.run();
        }
        return target;
    }

//TODO:
//    /**
//     * Sets the {@link ImageView} the image will be loaded into, cancels any existing loads into the view, and frees
//     * any resources Glide has loaded into the view so they may be reused.
//     *
//     * @see Glide#clear(View)
//     *
//     * @param view The view to cancel previous loads for and load the new image into.
//     * @return The {@link BitmapImageViewTarget} used to wrap the given {@link ImageView}.
//     */
//    public BitmapImageViewTarget into(ImageView view) {
//        return into(new BitmapImageViewTarget(view));
//    }

    private com.bumptech.glide.request.Request buildRequest(Target<ResourceType> target) {
        final com.bumptech.glide.request.Request result;

        if (priority == null) {
            priority = Priority.NORMAL;
        }

        if (thumbnailRequestBuilder != null) {
            ThumbnailRequestCoordinator requestCoordinator = new ThumbnailRequestCoordinator();
            com.bumptech.glide.request.Request fullRequest = buildRequest(target, sizeMultiplier, priority, requestCoordinator);

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

            com.bumptech.glide.request.Request thumbnailRequest = thumbnailRequestBuilder.buildRequest(target,
                    thumbnailRequestBuilder.sizeMultiplier, thumbnailRequestBuilder.priority, requestCoordinator);

            requestCoordinator.setRequests(fullRequest, thumbnailRequest);
            result = requestCoordinator;
        } else if (thumbSizeMultiplier != null) {
            ThumbnailRequestCoordinator requestCoordinator = new ThumbnailRequestCoordinator();
            com.bumptech.glide.request.Request fullRequest = buildRequest(target, sizeMultiplier, priority, requestCoordinator);
            com.bumptech.glide.request.Request thumbnailRequest = buildRequest(target, thumbSizeMultiplier, getThumbnailPriority(),
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

    private com.bumptech.glide.request.Request buildRequest(Target<ResourceType> target, float sizeMultiplier, Priority priority,
            RequestCoordinator requestCoordinator) {
        if (model == null) {
            return buildRequestForDataType(target, imageLoadProvider, sizeMultiplier, priority, null);
        }
        if (imageLoadProvider == null) {
            return buildRequestForDataType(target, videoLoadProvider, sizeMultiplier, priority, requestCoordinator);
        } else if (videoLoadProvider == null) {
            return buildRequestForDataType(target, imageLoadProvider, sizeMultiplier, priority, requestCoordinator);
        } else {
            MultiTypeRequestCoordinator typeCoordinator = new MultiTypeRequestCoordinator(requestCoordinator);
            com.bumptech.glide.request.Request imageRequest =
                    buildRequestForDataType(target, imageLoadProvider, sizeMultiplier, priority, typeCoordinator);
            com.bumptech.glide.request.Request videoRequest =
                    buildRequestForDataType(target, videoLoadProvider, sizeMultiplier, priority, typeCoordinator);
            typeCoordinator.setRequests(imageRequest, videoRequest);
            return typeCoordinator;
        }
    }

    private <Z> com.bumptech.glide.request.Request buildRequestForDataType(Target<ResourceType> target,
            LoadProvider<ModelType, Z, ResourceType> loadProvider, float sizeMultiplier, Priority priority,
            RequestCoordinator requestCoordinator) {
        return new GenericRequest<ModelType, Z, ResourceType>(loadProvider, model, context, priority, target,
                sizeMultiplier, placeholderDrawable, placeholderId, errorPlaceholder, errorId, requestListener,
                animationId, animation, requestCoordinator, Glide.get(context).getEngine(), getFinalTransformation());
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
