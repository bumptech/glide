package com.bumptech.glide.resize.request;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.animation.Animation;
import com.bumptech.glide.RequestListener;
import com.bumptech.glide.loader.bitmap.BitmapLoadFactory;
import com.bumptech.glide.resize.Engine;
import com.bumptech.glide.resize.ImageManager;
import com.bumptech.glide.resize.Priority;
import com.bumptech.glide.resize.RequestContext;
import com.bumptech.glide.resize.load.DecodeFormat;
import com.bumptech.glide.resize.target.Target;

/**
 * A simple builder class for {@link BitmapRequest}.
 *
 * @param <T, Z> The model type the {@link BitmapRequest} will load an {@link Bitmap} from.
 * @param <Z> The resource type the {@link BitmapRequest} will load an {@link Bitmap} from.
 */
public class BitmapRequestBuilder<T, Z> {
    Class<Z> resourceClass;
    T model;
    ImageManager imageManager;
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
    DecodeFormat decodeFormat = DecodeFormat.PREFER_RGB_565;
    Engine engine;
    RequestContext requestContext;

    public BitmapRequestBuilder(Class<Z> resourceClass) {
        this.resourceClass = resourceClass;
    }

    public BitmapRequestBuilder<T, Z> setModel(T model) {
        this.model = model;
        return this;
    }

    public BitmapRequestBuilder<T, Z> setContext(Context context) {
        this.context = context;
        return this;
    }

    public BitmapRequestBuilder<T, Z> setImageManager(ImageManager imageManager) {
        this.imageManager = imageManager;
        return this;
    }

    public BitmapRequestBuilder<T, Z> setDecodeFormat(DecodeFormat decodeFormat) {
        this.decodeFormat = decodeFormat;
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

    public BitmapRequestBuilder<T, Z> setRequestContext(RequestContext requestContext) {
        this.requestContext = requestContext;
        return this;
    }

    public BitmapRequest<T, Z> build() {
        return new BitmapRequest<T, Z>(this);
    }
}