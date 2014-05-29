package com.bumptech.glide.samples.flickr;

import android.content.Context;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelCache;
import com.bumptech.glide.load.model.GenericLoaderFactory;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.stream.BaseGlideUrlLoader;
import com.bumptech.glide.samples.flickr.api.Api;
import com.bumptech.glide.samples.flickr.api.Photo;

import java.io.InputStream;

/**
 * An implementation of ModelStreamLoader that leverages the StreamOpener class and the ExecutorService backing the
 * Engine to download the image and resize it in memory before saving the resized version
 * directly to the disk cache.
 */
public class FlickrModelLoader extends BaseGlideUrlLoader<Photo> {
    private final ModelCache<String> stringCache;

    public static class Factory implements ModelLoaderFactory<Photo, InputStream> {
        private final ModelCache<GlideUrl> modelCache = new ModelCache<GlideUrl>();
        private final ModelCache<String> stringCache = new ModelCache<String>();

        @Override
        public ModelLoader<Photo, InputStream> build(Context context, GenericLoaderFactory factories) {
            return new FlickrModelLoader(context, modelCache, stringCache);
        }

        @Override
        public void teardown() {
        }
    }

    public FlickrModelLoader(Context context, ModelCache<GlideUrl> modelCache, ModelCache<String> stringCache) {
        super(context, modelCache);
        this.stringCache = stringCache;
    }

    @Override
    protected String getUrl(Photo model, int width, int height) {
        final String id = getId(model);
        String result = stringCache.get(id, width, height);
        if (result == null) {
            result = Api.getPhotoURL(model, width, height);
            stringCache.put(id, width, height, result);
        }
        return result;
    }

    @Override
    public String getId(Photo model) {
        return model.id;
    }
}
