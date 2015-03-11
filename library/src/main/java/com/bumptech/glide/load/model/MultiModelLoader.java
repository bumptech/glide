package com.bumptech.glide.load.model;

import com.bumptech.glide.load.Options;

import java.util.Arrays;
import java.util.List;

class MultiModelLoader<Model, Data> implements ModelLoader<Model, Data> {

  private final List<ModelLoader<Model, Data>> modelLoaders;

  MultiModelLoader(List<ModelLoader<Model, Data>> modelLoaders) {
    this.modelLoaders = modelLoaders;
  }

  @Override
  public LoadData<Data> buildLoadData(Model model, int width, int height,
      Options options) {
    LoadData<Data> result = null;
    for (ModelLoader<Model, Data> modelLoader : modelLoaders) {
      if (modelLoader.handles(model)) {
        result = modelLoader.buildLoadData(model, width, height, options);
        if (result != null) {
          break;
        }
      }
    }
    return result;
  }

  @Override
  public boolean handles(Model model) {
    for (ModelLoader<Model, Data> modelLoader : modelLoaders) {
      if (modelLoader.handles(model)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return "MultiModelLoader{" + "modelLoaders=" + Arrays
        .toString(modelLoaders.toArray(new ModelLoader[modelLoaders.size()])) + '}';
  }
}
