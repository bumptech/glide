package com.bumptech.glide.load.model;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

class MultiModelLoader<Model, Data> implements ModelLoader<Model, Data> {

  private final List<ModelLoader<Model, Data>> modelLoaders;

  MultiModelLoader(List<ModelLoader<Model, Data>> modelLoaders) {
    this.modelLoaders = modelLoaders;
  }

  @Override
  public LoadData<Data> buildLoadData(Model model, int width, int height,
      Map<String, Object> options) {
    ModelLoader<Model, Data> bestLoader = getBestLoader(model);
    return bestLoader != null ? bestLoader.buildLoadData(model, width, height, options) : null;
  }

  @Override
  public boolean handles(Model model) {
    return getBestLoader(model) != null;
  }

  @Override
  public String toString() {
    return "MultiModelLoader{" + "modelLoaders=" + Arrays
        .toString(modelLoaders.toArray(new ModelLoader[modelLoaders.size()])) + '}';
  }

  private ModelLoader<Model, Data> getBestLoader(Model model) {
     for (ModelLoader<Model, Data> modelLoader : modelLoaders) {
      if (modelLoader.handles(model)) {
        return modelLoader;
      }
    }
    return null;
  }
}
