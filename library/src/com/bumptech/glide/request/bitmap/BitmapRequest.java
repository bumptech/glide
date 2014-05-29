package com.bumptech.glide.request.bitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.bumptech.glide.provider.LoadProvider;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.resource.ResourceFetcher;
import com.bumptech.glide.load.engine.Engine;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestCoordinator;
import com.bumptech.glide.Metadata;
import com.bumptech.glide.Priority;
import com.bumptech.glide.Resource;
import com.bumptech.glide.request.ResourceCallback;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.request.target.Target;

import java.io.InputStream;

/**
 * A {@link Request} that loads an {@link Bitmap} into a given {@link Target}.
 *
 * @param <T> The type of the model that the {@link Bitmap} will be loaded from.
 * @param <Z> The type of the resource that the {@link Bitmap} will be loaded from.
 */
public class BitmapRequest<T, Z> implements Request, Target.SizeReadyCallback, ResourceCallback<Bitmap> {
    private static final String TAG = "BitmapRequest";

    private final int placeholderResourceId;
    private final int errorResourceId;
    private final Context context;
    private final Transformation<Bitmap> transformation;
    private final LoadProvider<T, Z, Bitmap> loadProvider;
    private DecodeFormat decodeFormat;
    private final int animationId;
    private final RequestCoordinator requestCoordinator;
    private final T model;
    private Priority priority;
    private final Target target;
    private final RequestListener<T> requestListener;
    private final float sizeMultiplier;
    private final Engine engine;
    private Animation animation;
    private Drawable placeholderDrawable;
    private Drawable errorDrawable;
    private boolean isCancelled;
    private boolean isError;
    private boolean loadedFromMemoryCache;
    private Resource<Bitmap> resource;
    private Engine.LoadStatus loadStatus;

    public BitmapRequest(BitmapRequestBuilder<T, Z> builder) {
        this.loadProvider = builder.loadProvider;
        this.model = builder.model;
        this.context = builder.context.getApplicationContext();
        this.priority = builder.priority;
        this.target = builder.target;
        this.sizeMultiplier = builder.sizeMultiplier;
        this.placeholderDrawable = builder.placeholderDrawable;
        this.placeholderResourceId = builder.placeholderResourceId;
        this.errorDrawable = builder.errorDrawable;
        this.errorResourceId = builder.errorResourceId;
        this.requestListener = builder.requestListener;
        this.animationId = builder.animationId;
        this.animation = builder.animation;
        this.requestCoordinator = builder.requestCoordinator;
        this.decodeFormat = builder.decodeFormat;
        this.engine = builder.engine;
        this.transformation = builder.transformation;
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
            engine.recycle(resource);
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
        ResourceDecoder<InputStream, Bitmap> cacheDecoder = loadProvider.getCacheDecoder();
        ResourceDecoder<Z, Bitmap> decoder = loadProvider.getSourceDecoder();
        ResourceEncoder <Bitmap> encoder = loadProvider.getEncoder();
        ModelLoader<T, Z> modelLoader = loadProvider.getModelLoader();

        final String id = modelLoader.getId(model);
        final ResourceFetcher<Z> resourceFetcher = modelLoader.getResourceFetcher(model, width, height);
        final Metadata metadata = new Metadata(priority, decodeFormat);

        loadedFromMemoryCache = true;
        loadStatus = engine.load(id, width, height, cacheDecoder, resourceFetcher, decoder, transformation,
                encoder, metadata, this);
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
    public void onResourceReady(Resource<Bitmap> resource) {
        if (!canSetImage()) {
            engine.recycle(resource);
            return;
        }
        Bitmap loaded = resource.get();
        target.onImageReady(loaded);
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
