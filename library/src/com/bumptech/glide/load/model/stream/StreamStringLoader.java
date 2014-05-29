package com.bumptech.glide.load.model.stream;

import android.content.Context;
import android.net.Uri;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GenericLoaderFactory;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.StringLoader;

import java.io.InputStream;

/**
 * A {@link ModelLoader} For translating {@link String} models, such as file paths or remote urls, into
 * {@link InputStream} resources.
 */
public class StreamStringLoader extends StringLoader<InputStream> implements StreamModelLoader<String> {

    public static class Factory implements ModelLoaderFactory<String, InputStream> {
        @Override
        public ModelLoader<String, InputStream> build(Context context, GenericLoaderFactory factories) {
            return new StreamStringLoader(factories.buildModelLoader(Uri.class, InputStream.class, context));
        }

        @Override
        public void teardown() { }
    }

    public StreamStringLoader(Context context) {
        this(Glide.buildStreamModelLoader(Uri.class, context));
    }

    public StreamStringLoader(ModelLoader<Uri, InputStream> uriLoader) {
        super(uriLoader);
    }
}
