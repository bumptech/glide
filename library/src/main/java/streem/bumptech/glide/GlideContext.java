package com.bumptech.glide;

import android.content.Context;
import android.content.ContextWrapper;
import android.widget.ImageView;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.bumptech.glide.Glide.RequestOptionsFactory;
import com.bumptech.glide.load.engine.Engine;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.ImageViewTargetFactory;
import com.bumptech.glide.request.target.ViewTarget;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Global context for all loads in Glide containing and exposing the various registries and classes
 * required to load resources.
 */
@SuppressWarnings("PMD.DataClass")
public class GlideContext extends ContextWrapper {
  @VisibleForTesting
  static final TransitionOptions<?, ?> DEFAULT_TRANSITION_OPTIONS =
      new GenericTransitionOptions<>();

  private final ArrayPool arrayPool;
  private final Registry registry;
  private final ImageViewTargetFactory imageViewTargetFactory;
  private final RequestOptionsFactory defaultRequestOptionsFactory;
  private final List<RequestListener<Object>> defaultRequestListeners;
  private final Map<Class<?>, TransitionOptions<?, ?>> defaultTransitionOptions;
  private final Engine engine;
  private final GlideExperiments experiments;
  private final int logLevel;

  @Nullable
  @GuardedBy("this")
  private RequestOptions defaultRequestOptions;

  public GlideContext(
      @NonNull Context context,
      @NonNull ArrayPool arrayPool,
      @NonNull Registry registry,
      @NonNull ImageViewTargetFactory imageViewTargetFactory,
      @NonNull RequestOptionsFactory defaultRequestOptionsFactory,
      @NonNull Map<Class<?>, TransitionOptions<?, ?>> defaultTransitionOptions,
      @NonNull List<RequestListener<Object>> defaultRequestListeners,
      @NonNull Engine engine,
      @NonNull GlideExperiments experiments,
      int logLevel) {
    super(context.getApplicationContext());
    this.arrayPool = arrayPool;
    this.registry = registry;
    this.imageViewTargetFactory = imageViewTargetFactory;
    this.defaultRequestOptionsFactory = defaultRequestOptionsFactory;
    this.defaultRequestListeners = defaultRequestListeners;
    this.defaultTransitionOptions = defaultTransitionOptions;
    this.engine = engine;
    this.experiments = experiments;
    this.logLevel = logLevel;
  }

  public List<RequestListener<Object>> getDefaultRequestListeners() {
    return defaultRequestListeners;
  }

  public synchronized RequestOptions getDefaultRequestOptions() {
    if (defaultRequestOptions == null) {
      defaultRequestOptions = defaultRequestOptionsFactory.build().lock();
    }

    return defaultRequestOptions;
  }

  @SuppressWarnings("unchecked")
  @NonNull
  public <T> TransitionOptions<?, T> getDefaultTransitionOptions(@NonNull Class<T> transcodeClass) {
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

  @NonNull
  public <X> ViewTarget<ImageView, X> buildImageViewTarget(
      @NonNull ImageView imageView, @NonNull Class<X> transcodeClass) {
    return imageViewTargetFactory.buildTarget(imageView, transcodeClass);
  }

  @NonNull
  public Engine getEngine() {
    return engine;
  }

  @NonNull
  public Registry getRegistry() {
    return registry;
  }

  public int getLogLevel() {
    return logLevel;
  }

  @NonNull
  public ArrayPool getArrayPool() {
    return arrayPool;
  }

  public GlideExperiments getExperiments() {
    return experiments;
  }
}
