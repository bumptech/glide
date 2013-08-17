package com.bumptech.glide.samples.flickr;

import android.content.Context;
import com.bumptech.glide.loader.model.GenericLoaderFactory;
import com.bumptech.glide.loader.model.ModelLoaderFactory;
import com.bumptech.glide.loader.model.ModelLoader;
import com.bumptech.glide.loader.model.UrlModelLoader;
import com.bumptech.glide.samples.flickr.api.Api;
import com.bumptech.glide.samples.flickr.api.Photo;

import java.net.URL;

/**
 * An implementation of ModelStreamLoader that leverages the StreamOpener class and the ExecutorService backing the
 * ImageManager to download the image and resize it in memory before saving the resized version
 * directly to the disk cache.
 */
public class FlickrModelLoader extends UrlModelLoader<Photo> {

    public static class Factory implements ModelLoaderFactory<Photo> {

        @Override
        public ModelLoader<Photo> build(Context context, GenericLoaderFactory factories) {
            return new FlickrModelLoader(factories.buildModelLoader(URL.class, context));
        }

        @Override
        public Class<? extends ModelLoader<Photo>> loaderClass() {
            return FlickrModelLoader.class;
        }

        @Override
        public void teardown() { }
    }

    public FlickrModelLoader(ModelLoader<URL> concreteLoader) {
        super(concreteLoader );
    }

    @Override
    protected String getUrl(Photo model, int width, int height) {
        return Api.getPhotoURL(model, width, height);
    }

    @Override
    public String getId(Photo model) {
        return model.id;
    }
}
