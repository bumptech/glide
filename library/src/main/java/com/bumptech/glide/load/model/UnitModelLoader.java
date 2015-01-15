package com.bumptech.glide.load.model;

import android.content.Context;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;

public class UnitModelLoader<ResourceType> implements ModelLoader<ResourceType, ResourceType> {

    @Override
    public DataFetcher<ResourceType> getDataFetcher(ResourceType model, int width, int height) {
        return new UnitFetcher<ResourceType>(model);
    }

    @Override
    public boolean handles(ResourceType model) {
        return true;
    }

    private static class UnitFetcher<ResourceType> implements DataFetcher<ResourceType> {

        private ResourceType resource;

        public UnitFetcher(ResourceType resource) {
            this.resource = resource;
        }

        @Override
        public ResourceType loadData(Priority priority) throws Exception {
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
        public Class<ResourceType> getDataClass() {
            return (Class<ResourceType>) resource.getClass();
        }
    }

    public static class Factory<ResourceType> implements ModelLoaderFactory<ResourceType, ResourceType> {

        @Override
        public ModelLoader<ResourceType, ResourceType> build(Context context, MultiModelLoaderFactory multiFactory) {
            return new UnitModelLoader<ResourceType>();
        }

        @Override
        public void teardown() {
            // Do nothing.
        }
    }
}
