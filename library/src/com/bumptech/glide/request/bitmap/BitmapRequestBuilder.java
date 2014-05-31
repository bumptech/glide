package com.bumptech.glide.request.bitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.animation.Animation;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Engine;
import com.bumptech.glide.provider.LoadProvider;
import com.bumptech.glide.request.RequestCoordinator;
import com.bumptech.glide.request.target.Target;

/**
 * A simple builder class for {@link BitmapRequest}.
 *
 * @param <T> The model type the {@link BitmapRequest} will load an {@link Bitmap} from.
 * @param <Z> The resource type the {@link BitmapRequest} will load an {@link Bitmap} from.
 */
public class BitmapRequestBuilder<T, Z> {
    private static final BitmapRequestBuilder requestBuilder = new BitmapRequestBuilder();

    // Type erasure makes the types irrelevant at runtime.
    @SuppressWarnings("unchecked")
    public static <T, Z> BitmapRequestBuilder<T, Z> get() {
        return requestBuilder;
    }

    T model;
    Target target;
    Priority priority;
    float sizeMultiplier;
    Drawable placeholderDrawable;
    Drawable errorDrawable;
    RequestListener<T> requestListener;
    Animation animation;
    int placeholderResourceId;
    int errorResourceId;
    Context context;
    RequestCoordinator requestCoordinator;
    int animationId;
    Engine engine;
    Transformation<Bitmap> transformation;
    LoadProvider<T, Z, Bitmap> loadProvider;

    private void reset() {
        model = null;
        target = null;
        priority = null;
        sizeMultiplier = 0f;
        placeholderDrawable = null;
        placeholderResourceId = 0;
        requestListener = null;
        animation = null;
        placeholderResourceId = 0;
        errorResourceId = 0;
        context = null;
        requestCoordinator = null;
        animationId = 0;
        engine = null;
        transformation = null;
        loadProvider = null;
    }

    private BitmapRequestBuilder() {

    }

    public BitmapRequestBuilder<T, Z> setModel(T model) {
        this.model = model;
        return this;
    }

    public BitmapRequestBuilder<T, Z> setContext(Context context) {
        this.context = context;
        return this;
    }

    public BitmapRequestBuilder<T, Z> setTarget(Target target) {
        this.target = target;
        return this;
    }

    public BitmapRequestBuilder<T, Z> setPriority(Priority priority) {
        this.priority = priority;
        return this;
    }

    public BitmapRequestBuilder<T, Z> setSizeMultiplier(float sizeMultiplier) {
        this.sizeMultiplier = sizeMultiplier;
        return this;
    }

    public BitmapRequestBuilder<T, Z> setRequestListener(RequestListener<T> requestListener) {
        this.requestListener = requestListener;
        return this;
    }

    public BitmapRequestBuilder<T, Z> setAnimation(int animationId) {
        this.animationId = animationId;
        return this;
    }

    public BitmapRequestBuilder<T, Z> setAnimation(Animation animation) {
        this.animation = animation;
        return this;
    }

    public BitmapRequestBuilder<T, Z> setPlaceholderResource(int resourceId) {
        this.placeholderResourceId = resourceId;
        return this;
    }

    public BitmapRequestBuilder<T, Z> setPlaceholderDrawable(Drawable placeholderDrawable) {
        this.placeholderDrawable = placeholderDrawable;
        return this;
    }

    public BitmapRequestBuilder<T, Z> setErrorResource(int resourceId) {
        this.errorResourceId = resourceId;
        return this;
    }

    public BitmapRequestBuilder<T, Z> setErrorDrawable(Drawable errorDrawable) {
        this.errorDrawable = errorDrawable;
        return this;
    }

    public BitmapRequestBuilder<T, Z> setRequestCoordinator(RequestCoordinator requestCoordinator) {
        this.requestCoordinator = requestCoordinator;
        return this;
    }

    public BitmapRequestBuilder<T, Z> setEngine(Engine engine) {
        this.engine = engine;
        return this;
    }

    public BitmapRequestBuilder<T, Z> setLoadProvider(LoadProvider<T, Z, Bitmap> loadProvider) {
        this.loadProvider = loadProvider;
        return this;
    }

    public BitmapRequestBuilder<T, Z> setTransformation(Transformation<Bitmap> transformation) {
        this.transformation = transformation;
        return this;
    }

    public BitmapRequest<T, Z> build() {
        final BitmapRequest<T, Z> result = new BitmapRequest<T, Z>(this);
        reset();
        return result;
    }
}