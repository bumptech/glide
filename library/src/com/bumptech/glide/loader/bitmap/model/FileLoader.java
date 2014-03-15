package com.bumptech.glide.loader.bitmap.model;

import android.content.Context;
import android.net.Uri;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;

import java.io.File;
import java.io.InputStream;

/**
 * A simple model loader for {@link File}
 */
public class FileLoader implements ModelLoader<File, InputStream> {

    public static class Factory implements ModelLoaderFactory<File, InputStream> {
        @Override
        public ModelLoader<File, InputStream> build(Context context, GenericLoaderFactory factories) {
            return new FileLoader(factories.buildModelLoader(Uri.class, InputStream.class, context));
        }

        @Override
        public Class<? extends ModelLoader<File, InputStream>> loaderClass() {
            return FileLoader.class;
        }

        @Override
        public void teardown() { }
    }

    private final ModelLoader<Uri, InputStream> uriLoader;

    public FileLoader(ModelLoader<Uri, InputStream> uriLoader) {
        this.uriLoader = uriLoader;
    }

    @Override
    public ResourceFetcher<InputStream> getResourceFetcher(File model, int width, int height) {
        return uriLoader.getResourceFetcher(Uri.fromFile(model), width, height);
    }

    @Override
    public String getId(File model) {
        return model.getAbsolutePath();
    }
}
