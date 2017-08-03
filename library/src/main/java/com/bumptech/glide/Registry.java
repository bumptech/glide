package com.bumptech.glide;

import android.support.v4.util.Pools.Pool;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.data.DataRewinder;
import com.bumptech.glide.load.data.DataRewinderRegistry;
import com.bumptech.glide.load.engine.DecodePath;
import com.bumptech.glide.load.engine.LoadPath;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.ModelLoaderRegistry;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.load.resource.transcode.TranscoderRegistry;
import com.bumptech.glide.provider.EncoderRegistry;
import com.bumptech.glide.provider.ImageHeaderParserRegistry;
import com.bumptech.glide.provider.LoadPathCache;
import com.bumptech.glide.provider.ModelToResourceClassCache;
import com.bumptech.glide.provider.ResourceDecoderRegistry;
import com.bumptech.glide.provider.ResourceEncoderRegistry;
import com.bumptech.glide.util.pool.FactoryPools;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages component registration.
 */
public class Registry {
  private final ModelLoaderRegistry modelLoaderRegistry;
  private final EncoderRegistry encoderRegistry;
  private final ResourceDecoderRegistry decoderRegistry;
  private final ResourceEncoderRegistry resourceEncoderRegistry;
  private final DataRewinderRegistry dataRewinderRegistry;
  private final TranscoderRegistry transcoderRegistry;
  private final ImageHeaderParserRegistry imageHeaderParserRegistry;

  private final ModelToResourceClassCache modelToResourceClassCache =
      new ModelToResourceClassCache();
  private final LoadPathCache loadPathCache = new LoadPathCache();
  private final Pool<List<Exception>> exceptionListPool = FactoryPools.threadSafeList();

  public Registry() {
    this.modelLoaderRegistry = new ModelLoaderRegistry(exceptionListPool);
    this.encoderRegistry = new EncoderRegistry();
    this.decoderRegistry = new ResourceDecoderRegistry();
    this.resourceEncoderRegistry = new ResourceEncoderRegistry();
    this.dataRewinderRegistry = new DataRewinderRegistry();
    this.transcoderRegistry = new TranscoderRegistry();
    this.imageHeaderParserRegistry = new ImageHeaderParserRegistry();
  }

  public <Data> Registry register(Class<Data> dataClass, Encoder<Data> encoder) {
    encoderRegistry.add(dataClass, encoder);
    return this;
  }

  public <Data, TResource> Registry append(Class<Data> dataClass, Class<TResource> resourceClass,
      ResourceDecoder<Data, TResource> decoder) {
    decoderRegistry.append(decoder, dataClass, resourceClass);
    return this;
  }

  public <Data, TResource> Registry prepend(Class<Data> dataClass, Class<TResource> resourceClass,
      ResourceDecoder<Data, TResource> decoder) {
    decoderRegistry.prepend(decoder, dataClass, resourceClass);
    return this;
  }

  public <TResource> Registry register(Class<TResource> resourceClass,
      ResourceEncoder<TResource> encoder) {
    resourceEncoderRegistry.add(resourceClass, encoder);
    return this;
  }

  public Registry register(DataRewinder.Factory factory) {
    dataRewinderRegistry.register(factory);
    return this;
  }

  public <TResource, Transcode> Registry register(Class<TResource> resourceClass,
      Class<Transcode> transcodeClass, ResourceTranscoder<TResource, Transcode> transcoder) {
    transcoderRegistry.register(resourceClass, transcodeClass, transcoder);
    return this;
  }

  public Registry register(ImageHeaderParser parser) {
    imageHeaderParserRegistry.add(parser);
    return this;
  }

  /**
   * Use the given factory to build a {@link com.bumptech.glide.load.model.ModelLoader} for models
   * of the given class. Generally the best use of this method is to replace one of the default
   * factories or add an implementation for other similar low level models. Any factory replaced by
   * the given factory will have its {@link ModelLoaderFactory#teardown()}} method called.
   *
   * <p> Note - If a factory already exists for the given class, it will be replaced. If that
   * factory is not being used for any other model class, {@link ModelLoaderFactory#teardown()} will
   * be called. </p>
   *
   * <p> Note - The factory must not be an anonymous inner class of an Activity or another object
   * that cannot be retained statically. </p>
   *
   * @param modelClass The model class.
   * @param dataClass  the data class.
   */
  public <Model, Data> Registry append(Class<Model> modelClass, Class<Data> dataClass,
      ModelLoaderFactory<Model, Data> factory) {
    modelLoaderRegistry.append(modelClass, dataClass, factory);
    return this;
  }

  public <Model, Data> Registry prepend(Class<Model> modelClass, Class<Data> dataClass,
      ModelLoaderFactory<Model, Data> factory) {
    modelLoaderRegistry.prepend(modelClass, dataClass, factory);
    return this;
  }

  public <Model, Data> Registry replace(Class<Model> modelClass, Class<Data> dataClass,
      ModelLoaderFactory<Model, Data> factory) {
    modelLoaderRegistry.replace(modelClass, dataClass, factory);
    return this;
  }

  public <Data, TResource, Transcode> LoadPath<Data, TResource, Transcode> getLoadPath(
      Class<Data> dataClass, Class<TResource> resourceClass, Class<Transcode> transcodeClass) {
    LoadPath<Data, TResource, Transcode> result =
        loadPathCache.get(dataClass, resourceClass, transcodeClass);
    if (result == null && !loadPathCache.contains(dataClass, resourceClass, transcodeClass)) {
      List<DecodePath<Data, TResource, Transcode>> decodePaths =
          getDecodePaths(dataClass, resourceClass, transcodeClass);
      // It's possible there is no way to decode or transcode to the desired types from a given
      // data class.
      if (decodePaths.isEmpty()) {
        result = null;
      } else {
        result = new LoadPath<>(dataClass, resourceClass, transcodeClass, decodePaths,
            exceptionListPool);
      }
      loadPathCache.put(dataClass, resourceClass, transcodeClass, result);
    }
    return result;
  }

  private <Data, TResource, Transcode> List<DecodePath<Data, TResource, Transcode>> getDecodePaths(
      Class<Data> dataClass, Class<TResource> resourceClass, Class<Transcode> transcodeClass) {

    List<DecodePath<Data, TResource, Transcode>> decodePaths = new ArrayList<>();
    List<Class<TResource>> registeredResourceClasses =
        decoderRegistry.getResourceClasses(dataClass, resourceClass);

    for (Class<TResource> registeredResourceClass : registeredResourceClasses) {
      List<Class<Transcode>> registeredTranscodeClasses =
          transcoderRegistry.getTranscodeClasses(registeredResourceClass, transcodeClass);

      for (Class<Transcode> registeredTranscodeClass : registeredTranscodeClasses) {

        List<ResourceDecoder<Data, TResource>> decoders =
            decoderRegistry.getDecoders(dataClass, registeredResourceClass);
        ResourceTranscoder<TResource, Transcode> transcoder =
            transcoderRegistry.get(registeredResourceClass, registeredTranscodeClass);
        decodePaths.add(new DecodePath<>(dataClass, registeredResourceClass,
            registeredTranscodeClass, decoders, transcoder, exceptionListPool));
      }
    }
    return decodePaths;
  }

  public <Model, TResource, Transcode> List<Class<?>> getRegisteredResourceClasses(
      Class<Model> modelClass, Class<TResource> resourceClass, Class<Transcode> transcodeClass) {
    List<Class<?>> result = modelToResourceClassCache.get(modelClass, resourceClass);

    if (result == null) {
      result = new ArrayList<>();
      List<Class<?>> dataClasses = modelLoaderRegistry.getDataClasses(modelClass);
      for (Class<?> dataClass : dataClasses) {
        List<? extends Class<?>> registeredResourceClasses =
            decoderRegistry.getResourceClasses(dataClass, resourceClass);
        for (Class<?> registeredResourceClass : registeredResourceClasses) {
          List<Class<Transcode>> registeredTranscodeClasses = transcoderRegistry
              .getTranscodeClasses(registeredResourceClass, transcodeClass);
          if (!registeredTranscodeClasses.isEmpty() && !result.contains(registeredResourceClass)) {
              result.add(registeredResourceClass);
          }
        }
      }
      modelToResourceClassCache.put(modelClass, resourceClass,
          Collections.unmodifiableList(result));
    }

    return result;
  }

  public boolean isResourceEncoderAvailable(Resource<?> resource) {
    return resourceEncoderRegistry.get(resource.getResourceClass()) != null;
  }

  public <X> ResourceEncoder<X> getResultEncoder(Resource<X> resource)
      throws NoResultEncoderAvailableException {
    ResourceEncoder<X> resourceEncoder = resourceEncoderRegistry.get(resource.getResourceClass());
    if (resourceEncoder != null) {
      return resourceEncoder;
    }
    throw new NoResultEncoderAvailableException(resource.getResourceClass());
  }

  @SuppressWarnings("unchecked")
  public <X> Encoder<X> getSourceEncoder(X data) throws NoSourceEncoderAvailableException {
    Encoder<X> encoder = encoderRegistry.getEncoder((Class<X>) data.getClass());
    if (encoder != null) {
      return encoder;
    }
    throw new NoSourceEncoderAvailableException(data.getClass());
  }

  public <X> DataRewinder<X> getRewinder(X data) {
    return dataRewinderRegistry.build(data);
  }

  public <Model> List<ModelLoader<Model, ?>> getModelLoaders(Model model) {
    List<ModelLoader<Model, ?>> result = modelLoaderRegistry.getModelLoaders(model);
    if (result.isEmpty()) {
      throw new NoModelLoaderAvailableException(model);
    }
    return result;
  }

  public List<ImageHeaderParser> getImageHeaderParsers() {
    List<ImageHeaderParser> result = imageHeaderParserRegistry.getParsers();
    if (result.isEmpty()) {
      throw new NoImageHeaderParserException();
    }
    return result;
  }

  /**
   * Thrown when no {@link com.bumptech.glide.load.model.ModelLoader} is registered for a given
   * model class.
   */
  public static class NoModelLoaderAvailableException extends MissingComponentException {
    public NoModelLoaderAvailableException(Object model) {
      super("Failed to find any ModelLoaders for model: " + model);
    }

    public NoModelLoaderAvailableException(Class<?> modelClass, Class<?> dataClass) {
      super("Failed to find any ModelLoaders for model: " + modelClass + " and data: " + dataClass);
    }
  }

  /**
   * Thrown when no {@link ResourceEncoder} is registered for a given resource class.
   */
  public static class NoResultEncoderAvailableException extends MissingComponentException {
    public NoResultEncoderAvailableException(Class<?> resourceClass) {
      super("Failed to find result encoder for resource class: " + resourceClass);
    }
  }

  /**
   * Thrown when no {@link Encoder} is registered for a given data class.
   */
  public static class NoSourceEncoderAvailableException extends MissingComponentException {
    public NoSourceEncoderAvailableException(Class<?> dataClass) {
      super("Failed to find source encoder for data class: " + dataClass);
    }
  }

  /**
   * Thrown when some necessary component is missing for a load.
   */
  public static class MissingComponentException extends RuntimeException {
    public MissingComponentException(String message) {
      super(message);
    }
  }

  /**
   * Thrown when no {@link ImageHeaderParser} is registered.
   */
  public static final class NoImageHeaderParserException extends MissingComponentException {
    public NoImageHeaderParserException() {
      super("Failed to find image header parser.");
    }
  }
}
