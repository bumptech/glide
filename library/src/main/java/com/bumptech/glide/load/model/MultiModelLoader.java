package com.bumptech.glide.load.model;

import com.bumptech.glide.load.data.DataFetcher;

import java.util.Arrays;
import java.util.List;

class MultiModelLoader<Model, Data> implements ModelLoader<Model, Data> {

  private final List<ModelLoader<Model, Data>> modelLoaders;

  MultiModelLoader(List<ModelLoader<Model, Data>> modelLoaders) {
    this.modelLoaders = modelLoaders;
  }

  @Override
  public DataFetcher<Data> getDataFetcher(Model model, int width, int height) {
    DataFetcher<Data> result = null;
    for (ModelLoader<Model, Data> modelLoader : modelLoaders) {
      if (modelLoader.handles(model)) {
        result = modelLoader.getDataFetcher(model, width, height);
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
