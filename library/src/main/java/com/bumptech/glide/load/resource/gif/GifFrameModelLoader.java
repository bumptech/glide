package com.bumptech.glide.load.resource.gif;

import android.content.Context;

import com.bumptech.glide.Priority;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

public final class GifFrameModelLoader implements ModelLoader<GifDecoder, GifDecoder> {

    public static final class Factory implements ModelLoaderFactory<GifDecoder, GifDecoder> {

        public ModelLoader<GifDecoder, GifDecoder> build(Context context, MultiModelLoaderFactory multiFactory) {
            return new GifFrameModelLoader();
        }

        @Override
        public void teardown() {
            // Do nothing.
        }
    }

    GifFrameModelLoader() {
        // Package protected visibility.
    }

    @Override
    public DataFetcher<GifDecoder> getDataFetcher(GifDecoder model, int width, int height) {
        return new GifFrameDataFetcher(model);
    }

    @Override
    public boolean handles(GifDecoder model) {
        return true;
    }

    private static class GifFrameDataFetcher implements DataFetcher<GifDecoder> {
        private final GifDecoder decoder;

        public GifFrameDataFetcher(GifDecoder decoder) {
            this.decoder = decoder;
        }

        @Override
        public GifDecoder loadData(Priority priority) {
            return decoder;
        }

        @Override
        public void cleanup() {
            // Do nothing. GifDecoder reads from an arbitrary InputStream, the caller will close that stream.
        }

        @Override
        public String getId() {
            return String.valueOf(decoder.getCurrentFrameIndex());
        }

        @Override
        public void cancel() {
            // Do nothing.
        }
    }
}
