package com.bumptech.glide;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.widget.ImageView;
import com.bumptech.glide.load.engine.Engine;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.ImageViewTargetFactory;
import com.bumptech.glide.request.target.Target;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Global context for all loads in Glide containing and exposing the various registries and classes
 * required to load resources.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class GlideContext extends ContextWrapper {
  @VisibleForTesting
  static final TransitionOptions<?, ?> DEFAULT_TRANSITION_OPTIONS =
      new GenericTransitionOptions<Object>();
  private final Handler mainHandler;
  private final Registry registry;
  private final ImageViewTargetFactory imageViewTargetFactory;
  private final RequestOptions defaultRequestOptions;
  private final Map<Class<?>, TransitionOptions<?, ?>> defaultTransitionOptions;
  private final Engine engine;
  private final int logLevel;

  public GlideContext(Context context, Registry registry,
      ImageViewTargetFactory imageViewTargetFactory, RequestOptions defaultRequestOptions,
      Map<Class<?>, TransitionOptions<?, ?>> defaultTransitionOptions, Engine engine,
      int logLevel) {
    super(context.getApplicationContext());
    this.registry = registry;
    this.imageViewTargetFactory = imageViewTargetFactory;
    this.defaultRequestOptions = defaultRequestOptions;
    this.defaultTransitionOptions = defaultTransitionOptions;
    this.engine = engine;
    this.logLevel = logLevel;

    mainHandler = new Handler(Looper.getMainLooper());
  }

  public RequestOptions getDefaultRequestOptions() {
    return defaultRequestOptions;
  }

  @SuppressWarnings("unchecked")
  @NonNull
  public <T> TransitionOptions<?, T> getDefaultTransitionOptions(Class<T> transcodeClass) {
    TransitionOptions<?, ?> result = defaultTransitionOptions.get(transcodeClass);
    if (result == null) {
      for (Entry<Class<?>, TransitionOptions<?, ?>> value : defaultTransitionOptions.entrySet()) {
        if (value.getKey().isAssignableFrom(transcodeClass)) {
          result = value.getValue();
        }
      }
    }
    if (result == null) {
      result = DEFAULT_TRANSITION_OPTIONS;
    }
    return (TransitionOptions<?, T>) result;
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
