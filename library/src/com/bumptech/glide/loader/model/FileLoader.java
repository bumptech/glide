package com.bumptech.glide.loader.model;

import android.content.Context;
import android.net.Uri;
import com.bumptech.glide.loader.stream.StreamLoader;

import java.io.File;

/**
 * A simple model loader for {@link File}
 */
public class FileLoader implements ModelLoader<File> {

    public static class Factory implements ModelLoaderFactory<File> {
        @Override
        public ModelLoader<File> build(Context context, GenericLoaderFactory factories) {
            return new FileLoader(factories.buildModelLoader(Uri.class, context));
        }

        @Override
        public Class<? extends ModelLoader<File>> loaderClass() {
            return FileLoader.class;
        }

        @Override
        public void teardown() { }
    }

    private final ModelLoader<Uri> uriLoader;

    public FileLoader(ModelLoader<Uri> uriLoader) {
        this.uriLoader = uriLoader;
    }

    @Override
    public StreamLoader getStreamLoader(File model, int width, int height) {
        return uriLoader.getStreamLoader(Uri.fromFile(model), width, height);
    }

    @Override
    public String getId(File model) {
        //canonical is better, but also slower
        return model.getAbsolutePath();
    }
}
