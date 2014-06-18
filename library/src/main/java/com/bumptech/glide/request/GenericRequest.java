package com.bumptech.glide.request;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import com.bumptech.glide.Priority;
import com.bumptech.glide.Resource;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.Engine;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.provider.LoadProvider;
import com.bumptech.glide.request.target.Target;

import java.io.InputStream;

/**
 * A {@link Request} that loads a {@link Resource} into a given {@link Target}.
 *
 * @param <A> The type of the model that the resource will be loaded from.
 * @param <T> The type of the data that the resource will be loaded from.
 * @param <Z> The type of the resource that will be loaded.
 */
public class GenericRequest<A, T, Z, R> implements Request, Target.SizeReadyCallback, ResourceCallback {
    private static final String TAG = "Request";

    private final int placeholderResourceId;
    private final int errorResourceId;
    private final Context context;
    private final Transformation<Z> transformation;
    private final LoadProvider<A, T, Z, R> loadProvider;
    private final RequestCoordinator requestCoordinator;
    private final A model;
    private final Class<R> transcodeClass;
    private final boolean isMemoryCacheable;
    private final Priority priority;
    private final Target<R> target;
    private final RequestListener<A, R> requestListener;
    private final float sizeMultiplier;
    private final Engine engine;
    private final GlideAnimationFactory<R> animationFactory;

    private Drawable placeholderDrawable;
    private Drawable errorDrawable;
    private boolean isCancelled;
    private boolean isError;
    private boolean loadedFromMemoryCache;
    private Resource resource;
    private Engine.LoadStatus loadStatus;
    private boolean isRunning;

    public GenericRequest(
            LoadProvider<A, T, Z, R> loadProvider,
            A model,
            Context context,
            Priority priority,
            Target<R> target,
            float sizeMultiplier,
            Drawable placeholderDrawable,
            int placeholderResourceId,
            Drawable errorDrawable,
            int errorResourceId,
            RequestListener<A, R> requestListener,
            RequestCoordinator requestCoordinator,
            Engine engine,
            Transformation<Z> transformation,
            Class<R> transcodeClass,
            boolean isMemoryCacheable,
            GlideAnimationFactory<R> animationFactory) {
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
        this.requestCoordinator = requestCoordinator;
        this.engine = engine;
        this.transformation = transformation;
        this.transcodeClass = transcodeClass;
        this.isMemoryCacheable = isMemoryCacheable;
        this.animationFactory = animationFactory;

        // We allow null models by just setting an error drawable. Null models will always have empty providers, we
        // simply skip our sanity checks in that unusual case.
        if (model != null) {
            if (loadProvider.getCacheDecoder() == null) {
                throw new NullPointerException("CacheDecoder must not be null, try .cacheDecoder(ResouceDecoder)");
            }
            if (loadProvider.getSourceDecoder() == null) {
                throw new NullPointerException("SourceDecoder must not be null, try .imageDecoder(ResourceDecoder) " +
                        "and/or .videoDecoder()");
            }
            if (loadProvider.getEncoder() == null) {
                throw new NullPointerException("Encoder must not be null, try .encode(ResourceEncoder)");
            }
            if (loadProvider.getTranscoder() == null) {
                throw new NullPointerException("Transcoder must not be null, try .as(Class, ResourceTranscoder)");
            }
            if (loadProvider.getModelLoader() == null) {
                throw new NullPointerException("ModelLoader must not be null, try .using(ModelLoader)");
            }
        }
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
            isRunning = true;
        }
    }

    public void cancel() {
        isRunning = false;
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
    public boolean isRunning() {
        return isRunning;
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

        target.setPlaceholder(getPlaceholderDrawable());
    }

    private void setErrorPlaceholder() {
        if (!canSetPlaceholder()) return;

        Drawable error = getErrorDrawable();
        if (error != null) {
            target.setPlaceholder(error);
        } else {
            setPlaceHolder();
        }
    }

    private Drawable getErrorDrawable() {
        if (errorDrawable == null && errorResourceId > 0) {
            errorDrawable = context.getResources().getDrawable(errorResourceId);
        }
        return errorDrawable;
    }

    private Drawable getPlaceholderDrawable() {
        if (placeholderDrawable == null && placeholderResourceId > 0) {
            placeholderDrawable = context.getResources().getDrawable(placeholderResourceId);
        }
        return placeholderDrawable;
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
        ResourceTranscoder<Z, R> transcoder = loadProvider.getTranscoder();
        ModelLoader<A, T> modelLoader = loadProvider.getModelLoader();

        final String id = modelLoader.getId(model);
        final DataFetcher<T> dataFetcher = modelLoader.getResourceFetcher(model, width, height);

        loadedFromMemoryCache = true;
        loadStatus = engine.load(id, width, height, cacheDecoder, dataFetcher, decoder, transformation,
                encoder, transcoder, priority, isMemoryCacheable, this);
        loadedFromMemoryCache = resource != null;
    }

    private boolean canSetImage() {
        return requestCoordinator == null || requestCoordinator.canSetImage(this);
    }

    private boolean canSetPlaceholder() {
        return requestCoordinator == null || requestCoordinator.canSetPlaceholder(this);
    }

    private boolean isFirstImage() {
        return requestCoordinator == null || !requestCoordinator.isAnyRequestComplete();
    }

    @Override
    public void onResourceReady(Resource resource) {
        isRunning = false;
        if (!canSetImage()) {
            resource.release();
            return;
        }
        if (resource == null || !transcodeClass.isAssignableFrom(resource.get().getClass())) {
            if (resource != null) {
                resource.release();
            }
            onException(new Exception("Expected to receive an object of " + transcodeClass + " but instead got " +
                    (resource != null ? resource.get() : null)));
            return;
        }
        R result = (R) resource.get();

        if (requestListener == null || !requestListener.onResourceReady(result, model, target, loadedFromMemoryCache,
                isFirstImage())) {
            GlideAnimation<R> animation = animationFactory.build(loadedFromMemoryCache, isFirstImage());
            target.onResourceReady(result, animation);
        }

        this.resource = resource;
    }

    @Override
    public void onException(Exception e) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "load failed", e);
        }

        isRunning = false;
        isError = true;
        //TODO: what if this is a thumbnail request?
        if (requestListener == null || !requestListener.onException(e, model, target, isFirstImage())) {
            setErrorPlaceholder();
        }
    }
}
