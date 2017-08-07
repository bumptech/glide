package com.bumptech.glide;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import com.bumptech.glide.load.engine.Engine;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.ImageViewTargetFactory;
import com.bumptech.glide.request.target.Target;

/**
 * Global context for all loads in Glide containing and exposing the various registries and classes
 * required to load resources.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class GlideContext extends ContextWrapper {
  private final Handler mainHandler;
  private final Registry registry;
  private final ImageViewTargetFactory imageViewTargetFactory;
  private final RequestOptions defaultRequestOptions;
  private final Engine engine;
  private final int logLevel;

  public GlideContext(Context context, Registry registry,
      ImageViewTargetFactory imageViewTargetFactory, RequestOptions defaultRequestOptions,
      Engine engine, int logLevel) {
    super(context.getApplicationContext());
    this.registry = registry;
    this.imageViewTargetFactory = imageViewTargetFactory;
    this.defaultRequestOptions = defaultRequestOptions;
    this.engine = engine;
    this.logLevel = logLevel;

    mainHandler = new Handler(Looper.getMainLooper());
  }

  public RequestOptions getDefaultRequestOptions() {
    return defaultRequestOptions;
  }

  public <X> Target<X> buildImageViewTarget(ImageView imageView, Class<X> transcodeClass) {
    return imageViewTargetFactory.buildTarget(imageView, transcodeClass);
  }

  public Handler getMainHandler() {
    return mainHandler;
  }

  public Engine getEngine() {
    return engine;
  }

  public Registry getRegistry() {
    return registry;
  }

  public int getLogLevel() {
    return logLevel;
  }
}
