package com.bumptech.glide.request.bitmap;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.bumptech.glide.Priority;
import com.bumptech.glide.Resource;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Engine;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.resource.ResourceFetcher;
import com.bumptech.glide.provider.LoadProvider;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestCoordinator;
import com.bumptech.glide.request.ResourceCallback;
import com.bumptech.glide.request.target.Target;

import java.io.InputStream;

/**
 * A {@link Request} that loads a {@link Resource} into a given {@link Target}.
 *
 * @param <A> The type of the model that the resource will be loaded from.
 * @param <T> The type of the data that the resource will be loaded from.
 * @param <Z> The type of the resource that will be loaded.
 */
public class GenericRequest<A, T, Z> implements Request, Target.SizeReadyCallback, ResourceCallback {
    private static final String TAG = "Request";

    private final int placeholderResourceId;
    private final int errorResourceId;
    private final Context context;
    private final Transformation<Z> transformation;
    private final LoadProvider<A, T, Z> loadProvider;
    private final int animationId;
    private final RequestCoordinator requestCoordinator;
    private final A model;
    private final Class<Z> resourceClass;
    private Priority priority;
    private final Target<Z> target;
    private final RequestListener<A> requestListener;
    private final float sizeMultiplier;
    private final Engine engine;
    private Animation animation;
    private Drawable placeholderDrawable;
    private Drawable errorDrawable;
    private boolean isCancelled;
    private boolean isError;
    private boolean loadedFromMemoryCache;
    private Resource resource;
    private Engine.LoadStatus loadStatus;

    public GenericRequest(LoadProvider<A, T, Z> loadProvider, A model, Context context, Priority priority,
            Target<Z> target, float sizeMultiplier, Drawable placeholderDrawable, int placeholderResourceId,
            Drawable errorDrawable, int errorResourceId, RequestListener<A> requestListener, int animationId,
            Animation animation, RequestCoordinator requestCoordinator, Engine engine,
            Transformation<Z> transformation, Class<Z> resourceClass) {
        this.loadProvider = loadProvider;
        this.model = model;
        this.context = context;
        this.priority = priority;
        this.target = target;
        this.sizeMultiplier = sizeMultiplier;
        this.placeholderDrawable = placeholderDrawable;
        this.placeholderResourceId = placeholderResourceId;
        this.errorDrawable = errorDrawable;
        this.errorResourceId = errorResourceId;
        this.requestListener = requestListener;
        this.animationId = animationId;
        this.animation = animation;
        this.requestCoordinator = requestCoordinator;
        this.engine = engine;
        this.transformation = transformation;
        this.resourceClass = resourceClass;
    }

    @Override
    public void run() {
        if (model == null) {
            onException(null);
            return;
        }

        target.getSize(this);

        if (!isComplete() && !isFailed()) {
            setPlaceHolder();
        }
    }

    public void cancel() {
        isCancelled = true;
        if (loadStatus != null) {
            loadStatus.cancel();
            loadStatus = null;
        }
    }

    @Override
    public void clear() {
        cancel();
        setPlaceHolder();
        if (resource != null) {
            resource.release();
            resource = null;
        }
    }

    @Override
    public boolean isComplete() {
        return resource != null;
    }

    @Override
    public boolean isFailed() {
        return isError;
    }

    private void setPlaceHolder() {
        if (!canSetPlaceholder()) return;

        if (placeholderDrawable == null && placeholderResourceId > 0) {
            placeholderDrawable = context.getResources().getDrawable(placeholderResourceId);
        }
        target.setPlaceholder(placeholderDrawable);
    }

    private void setErrorPlaceholder() {
        if (!canSetPlaceholder()) return;

        if (errorDrawable == null && errorResourceId > 0) {
            errorDrawable = context.getResources().getDrawable(errorResourceId);
        }
        if (errorDrawable == null) {
            setPlaceHolder();
        } else {
            target.setPlaceholder(errorDrawable);
        }
    }

    @Override
    public void onSizeReady(int width, int height) {
        if (isCancelled) {
            return;
        }

        width = Math.round(sizeMultiplier * width);
        height = Math.round(sizeMultiplier * height);
        ResourceDecoder<InputStream, Z> cacheDecoder = loadProvider.getCacheDecoder();
        ResourceDecoder<T, Z> decoder = loadProvider.getSourceDecoder();
        ResourceEncoder <Z> encoder = loadProvider.getEncoder();
        ModelLoader<A, T> modelLoader = loadProvider.getModelLoader();

        final String id = modelLoader.getId(model);
        final ResourceFetcher<T> resourceFetcher = modelLoader.getResourceFetcher(model, width, height);

        loadedFromMemoryCache = true;
        loadStatus = engine.load(id, width, height, cacheDecoder, resourceFetcher, decoder, transformation,
                encoder, priority, this);
        loadedFromMemoryCache = resource != null;
    }

    private boolean canSetImage() {
        return requestCoordinator == null || requestCoordinator.canSetImage(this);
    }

    private boolean canSetPlaceholder() {
        return requestCoordinator == null || requestCoordinator.canSetPlaceholder(this);
    }

    private boolean isAnyImageSet() {
        return requestCoordinator != null && requestCoordinator.isAnyRequestComplete();
    }

    @Override
    public void onResourceReady(Resource resource) {
        if (!canSetImage()) {
            resource.release();
            return;
        }
        if (resource == null || !resourceClass.isAssignableFrom(resource.get().getClass())) {
            if (resource != null) {
                resource.release();
            }
            onException(new Exception("Expected to receive an object of " + resourceClass + " but instead got " +
                    (resource != null ? resource.get() : null)));
            return;
        }
        Z result = (Z) resource.get();
        target.onResourceReady(result);
        if (!loadedFromMemoryCache && !isAnyImageSet()) {
            if (animation == null && animationId > 0) {
                animation = AnimationUtils.loadAnimation(context, animationId);
            }
            if (animation != null) {
                target.startAnimation(animation);
            }
        }
        if (requestListener != null) {
            requestListener.onImageReady(model, target, loadedFromMemoryCache, isAnyImageSet());
        }

        this.resource = resource;
    }

    @Override
    public void onException(Exception e) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "load failed", e);
        }

        isError = true;
        setErrorPlaceholder();

        //TODO: what if this is a thumbnail request?
        if (requestListener != null) {
            requestListener.onException(e, model, target);
        }
    }
}
