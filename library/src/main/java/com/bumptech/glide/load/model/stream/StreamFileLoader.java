package com.bumptech.glide.load.model.stream;

import android.content.Context;
import android.net.Uri;
import com.bumptech.glide.load.model.FileLoader;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

import java.io.File;
import java.io.InputStream;

/**
 * A {@link ModelLoader} For translating {@link File} models for local uris into {@link InputStream} data.
 */
public class StreamFileLoader extends FileLoader<InputStream> implements StreamModelLoader<File> {

    /**
     * The default factory for {@link com.bumptech.glide.load.model.stream.StreamFileLoader}s.
     */
    public static class Factory implements ModelLoaderFactory<File, InputStream> {
        @Override
        public ModelLoader<File, InputStream> build(Context context, MultiModelLoaderFactory multiFactory) {
            return new StreamFileLoader(multiFactory.build(Uri.class, InputStream.class));
        }

        @Override
        public void teardown() {
            // Do nothing.
        }
    }

    protected StreamFileLoader(ModelLoader<Uri, InputStream> uriLoader) {
        super(uriLoader);
    }
}
