package com.bumptech.glide.load.model.stream;

import android.content.Context;

import com.bumptech.glide.load.data.ByteArrayFetcher;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

import java.io.InputStream;

/**
 * A base class to convert byte arrays to input streams so they can be decoded. This class is abstract because there is
 * no simple/quick way to generate an id from the bytes themselves, so subclass must include an id.
 */
public class ByteArrayLoader implements ModelLoader<byte[], InputStream> {

    @Override
    public DataFetcher<InputStream> getDataFetcher(byte[] model, int width, int height) {
        return new ByteArrayFetcher(model);
    }

    @Override
    public boolean handles(byte[] model) {
        return true;
    }

    /**
     * Factory for {@link ByteArrayLoader}.
     */
    public static class StreamFactory implements ModelLoaderFactory<byte[], InputStream> {

        @Override
        public ModelLoader<byte[], InputStream> build(Context context, MultiModelLoaderFactory multiFactory) {
            return new ByteArrayLoader();
        }

        @Override
        public void teardown() {
            // Do nothing.
        }
    }
}
