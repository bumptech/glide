package com.bumptech.glide.load.model;

import android.content.Context;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;

/**
 * A set of helper classes that performs no loading and instead always returns the given model as the data to decode.
 *
 * @param <ModelType> The type of model that will also be returned as decodable data.
 */
public class UnitModelLoader<ModelType> implements ModelLoader<ModelType, ModelType> {

    @Override
    public DataFetcher<ModelType> getDataFetcher(ModelType model, int width, int height) {
        return new UnitFetcher<>(model);
    }

    @Override
    public boolean handles(ModelType model) {
        return true;
    }

    private static class UnitFetcher<ModelType> implements DataFetcher<ModelType> {

        private final ModelType resource;

        public UnitFetcher(ModelType resource) {
            this.resource = resource;
        }

        @Override
        public ModelType loadData(Priority priority) throws Exception {
            return resource;
        }

        @Override
        public void cleanup() {
            // Do nothing.
        }

        @Override
        public String getId() {
            return resource.toString();
        }

        @Override
        public void cancel() {
            // Do nothing.
        }

        @SuppressWarnings("unchecked")
        @Override
        public Class<ModelType> getDataClass() {
            return (Class<ModelType>) resource.getClass();
        }

        @Override
        public DataSource getDataSource() {
            return DataSource.LOCAL;
        }
    }

    /**
     * Factory for producing {@link com.bumptech.glide.load.model.UnitModelLoader}s.
     */
    public static class Factory<ResourceType> implements ModelLoaderFactory<ResourceType, ResourceType> {

        @Override
        public ModelLoader<ResourceType, ResourceType> build(Context context, MultiModelLoaderFactory multiFactory) {
            return new UnitModelLoader<>();
        }

        @Override
        public void teardown() {
            // Do nothing.
        }
    }
}
