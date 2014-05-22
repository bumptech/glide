package com.bumptech.glide.resize.request;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.bumptech.glide.RequestListener;
import com.bumptech.glide.loader.bitmap.BitmapLoadFactory;
import com.bumptech.glide.resize.ImageManager;
import com.bumptech.glide.resize.Metadata;
import com.bumptech.glide.resize.Priority;
import com.bumptech.glide.resize.load.BitmapLoad;
import com.bumptech.glide.resize.load.DecodeFormat;
import com.bumptech.glide.resize.target.Target;

/**
 * A {@link Request} that loads an {@link Bitmap} into a given {@link Target}.
 *
 * @param <T> The type of the model that the {@link Bitmap} will be loaded from.
 */
public class BitmapRequest<T> implements Request, ImageManager.LoadedCallback, Target.SizeReadyCallback {
    private static final String TAG = "BitmapRequest";

    private final int placeholderResourceId;
    private final int errorResourceId;
    private final Context context;
    private final DecodeFormat decodeFormat;
    private final int animationId;
    private final RequestCoordinator requestCoordinator;
    private final T model;
    private final BitmapLoadFactory<T> bitmapLoadFactory;
    private final ImageManager imageManager;
    private final Priority priority;
    private final Target target;
    private final RequestListener<T> requestListener;
    private final float sizeMultiplier;

    private Animation animation;
    private Drawable placeholderDrawable;
    private Drawable errorDrawable;
    private boolean isCancelled;
    private boolean isError;
    private boolean loadedFromMemoryCache;
    private Bitmap bitmap;
    private ImageManager.LoadToken token;

    public BitmapRequest(BitmapRequestBuilder<T> builder) {
        this.model = builder.model;
        this.context = builder.context.getApplicationContext();
        this.bitmapLoadFactory = builder.bitmapLoadFactory;
        this.imageManager = builder.imageManager;
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
    }

    @Override
    public void run() {
        target.getSize(this);

        if (bitmap == null && !isError) {
            setPlaceHolder();
        }
    }

    public void cancel() {
        isCancelled = true;
        if (token != null) {
            token.cancel();
        }
    }

    public void clear() {
        cancel();
        setPlaceHolder();
        if (bitmap != null) {
            imageManager.releaseBitmap(bitmap);
            bitmap = null;
        }
    }

    @Override
    public boolean isComplete() {
        return bitmap != null;
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
        target.setPlaceholder(errorDrawable);
    }

    @Override
    public void onLoadCompleted(Bitmap loaded) {
        if (isCancelled || !canSetImage()) {
            imageManager.releaseBitmap(loaded);
            return;
        }
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

        bitmap = loaded;
    }

    @Override
    public void onLoadFailed(Exception e) {
        if (isCancelled) {
            return;
        } else {
            Log.e(TAG, "load failed", e);
        }

        isError = true;
        setErrorPlaceholder();

        //TODO: what if this is a thumbnail request?
        if (requestListener != null) {
            requestListener.onException(e, model, target);
        }
    }

    @Override
    public void onSizeReady(int width, int height) {
        // This should only be called once.
        if (isCancelled) {
            return;
        }
        width = Math.round(sizeMultiplier * width);
        height = Math.round(sizeMultiplier * height);
        BitmapLoad loadTask = bitmapLoadFactory.getLoadTask(model, width, height);
        if (loadTask == null) {
            if (model != null && Log.isLoggable(TAG, Log.INFO)) {
                Log.i(TAG, "Got null load task for model=" + model);
            }
            onLoadFailed(null);
            return;
        }

        loadTask.setMetadata(new Metadata(priority, decodeFormat));

        loadedFromMemoryCache = true;
        token = imageManager.getImage(loadTask, this);
        loadedFromMemoryCache = bitmap != null;
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
}
