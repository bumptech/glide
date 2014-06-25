package com.bumptech.glide.request;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.Engine;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.provider.LoadProvider;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.util.LogTime;

import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * A {@link Request} that loads a {@link Resource} into a given {@link Target}.
 *
 * @param <A> The type of the model that the resource will be loaded from.
 * @param <T> The type of the data that the resource will be loaded from.
 * @param <Z> The type of the resource that will be loaded.
 */
public class GenericRequest<A, T, Z, R> implements Request, Target.SizeReadyCallback, ResourceCallback {
    private static final String TAG = "GenericRequest";

    private int placeholderResourceId;
    private int errorResourceId;
    private Context context;
    private Transformation<Z> transformation;
    private LoadProvider<A, T, Z, R> loadProvider;
    private RequestCoordinator requestCoordinator;
    private A model;
    private Class<R> transcodeClass;
    private boolean isMemoryCacheable;
    private Priority priority;
    private Target<R> target;
    private RequestListener<A, R> requestListener;
    private float sizeMultiplier;
    private Engine engine;
    private GlideAnimationFactory<R> animationFactory;
    private int overrideWidth;
    private int overrideHeight;
    private String tag = String.valueOf(hashCode());
    private boolean cacheSource;

    private Drawable placeholderDrawable;
    private Drawable errorDrawable;
    private boolean isCancelled;
    private boolean isError;
    private boolean loadedFromMemoryCache;
    private Resource resource;
    private Engine.LoadStatus loadStatus;
    private boolean isRunning;
    private long startTime;

    private static final Queue<GenericRequest> queue = new ArrayDeque<GenericRequest>();

    @SuppressWarnings("unchecked")
    public static <A, T, Z, R> GenericRequest<A, T, Z, R> obtain(
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
            GlideAnimationFactory<R> animationFactory,
            int overrideWidth,
            int overrideHeight,
            boolean cacheSource) {
        GenericRequest request = queue.poll();
        if (request == null) {
            request = new GenericRequest();
        }
        request.init(loadProvider,
                model,
                context,
                priority,
                target,
                sizeMultiplier,
                placeholderDrawable,
                placeholderResourceId,
                errorDrawable,
                errorResourceId,
                requestListener,
                requestCoordinator,
                engine,
                transformation,
                transcodeClass,
                isMemoryCacheable,
                animationFactory,
                overrideWidth,
                overrideHeight,
                cacheSource);
        return request;
    }

    private GenericRequest() {

    }

    @Override
    public void recycle() {
        loadProvider = null;
        model = null;
        context = null;
        target = null;
        placeholderDrawable = null;
        errorDrawable = null;
        requestListener = null;
        requestCoordinator = null;
        engine = null;
        transformation = null;
        animationFactory = null;
        isCancelled = false;
        isError = false;
        loadedFromMemoryCache = false;
        loadStatus = null;
        isRunning = false;
        cacheSource = false;
        queue.offer(this);
    }


    private void init(
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
            GlideAnimationFactory<R> animationFactory,
            int overrideWidth,
            int overrideHeight,
            boolean cacheSource) {
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
        this.overrideWidth = overrideWidth;
        this.overrideHeight = overrideHeight;
        this.cacheSource = cacheSource;

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
            if (loadProvider.getSourceEncoder() == null) {
                throw new NullPointerException("SourceEncoder must not be null, try .sourceEncoder(Encoder)");
            }
        }
    }

    @Override
    public void run() {
        startTime = LogTime.getLogTime();
        if (model == null) {
            onException(null);
            return;
        }

        if (overrideWidth > 0 && overrideHeight > 0) {
            onSizeReady(overrideWidth, overrideHeight);
        } else {
            target.getSize(this);
        }

        if (!isComplete() && !isFailed()) {
            setPlaceHolder();
            isRunning = true;
        }
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            logV("finished run method in " + LogTime.getElapsedMillis(startTime));
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
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            logV("Got onSizeReady in " + LogTime.getElapsedMillis(startTime));
        }
        if (isCancelled) {
            return;
        }

        width = Math.round(sizeMultiplier * width);
        height = Math.round(sizeMultiplier * height);
        ResourceDecoder<InputStream, Z> cacheDecoder = loadProvider.getCacheDecoder();
        Encoder<T> sourceEncoder = loadProvider.getSourceEncoder();
        ResourceDecoder<T, Z> decoder = loadProvider.getSourceDecoder();
        ResourceEncoder <Z> encoder = loadProvider.getEncoder();
        ResourceTranscoder<Z, R> transcoder = loadProvider.getTranscoder();
        ModelLoader<A, T> modelLoader = loadProvider.getModelLoader();

        final DataFetcher<T> dataFetcher = modelLoader.getResourceFetcher(model, width, height);

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            logV("finished setup for calling load in " + LogTime.getElapsedMillis(startTime));
        }
        loadedFromMemoryCache = true;
        loadStatus = engine.load(width, height, cacheDecoder, dataFetcher, cacheSource, sourceEncoder, decoder,
                transformation, encoder, transcoder, priority, isMemoryCacheable, this);
        loadedFromMemoryCache = resource != null;
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            logV("finished onSizeReady in " + LogTime.getElapsedMillis(startTime));
        }
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

    @SuppressWarnings("unchecked")
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

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            logV("Resource ready in " + LogTime.getElapsedMillis(startTime) + " size: "
                    + (resource.getSize() / (1024d * 1024d)) + " fromCache: " + loadedFromMemoryCache);
        }
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

    private void logV(String message) {
        Log.v(TAG, message + " this: " + tag);
    }
}
