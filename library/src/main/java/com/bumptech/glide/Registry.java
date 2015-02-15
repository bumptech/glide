package com.bumptech.glide;

import android.content.Context;

import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.data.DataRewinder;
import com.bumptech.glide.load.data.DataRewinderRegistry;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.ModelLoaderRegistry;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.load.resource.transcode.TranscoderRegistry;
import com.bumptech.glide.provider.EncoderRegistry;
import com.bumptech.glide.provider.ResourceDecoderRegistry;
import com.bumptech.glide.provider.ResourceEncoderRegistry;

/**
 * Manages component registration.
 */
public class Registry {
  final ModelLoaderRegistry modelLoaderRegistry;
  final EncoderRegistry encoderRegistry;
  final ResourceDecoderRegistry decoderRegistry;
  final ResourceEncoderRegistry resourceEncoderRegistry;
  final DataRewinderRegistry dataRewinderRegistry;
  final TranscoderRegistry transcoderRegistry;

  public Registry(Context context) {
    this.modelLoaderRegistry = new ModelLoaderRegistry(context.getApplicationContext());
    this.encoderRegistry = new EncoderRegistry();
    this.decoderRegistry = new ResourceDecoderRegistry();
    this.resourceEncoderRegistry = new ResourceEncoderRegistry();
    this.dataRewinderRegistry = new DataRewinderRegistry();
    this.transcoderRegistry = new TranscoderRegistry();
  }

  public <Data> Registry register(Class<Data> dataClass, Encoder<Data> encoder) {
    encoderRegistry.add(dataClass, encoder);
    return this;
  }

  public <Data, Resource> Registry append(Class<Data> dataClass, Class<Resource> resourceClass,
      ResourceDecoder<Data, Resource> decoder) {
    decoderRegistry.append(decoder, dataClass, resourceClass);
    return this;
  }

  public <Data, Resource> Registry prepend(Class<Data> dataClass, Class<Resource> resourceClass,
      ResourceDecoder<Data, Resource> decoder) {
    decoderRegistry.prepend(decoder, dataClass, resourceClass);
    return this;
  }

  public <Resource> Registry register(Class<Resource> resourceClass,
      ResourceEncoder<Resource> encoder) {
    resourceEncoderRegistry.add(resourceClass, encoder);
    return this;
  }

  public Registry register(DataRewinder.Factory factory) {
    dataRewinderRegistry.register(factory);
    return this;
  }

  public <Resource, Transcode> Registry register(Class<Resource> resourceClass,
      Class<Transcode> transcodeClass, ResourceTranscoder<Resource, Transcode> transcoder) {
    transcoderRegistry.register(resourceClass, transcodeClass, transcoder);
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
}
