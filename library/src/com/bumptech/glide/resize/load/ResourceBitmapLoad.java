package com.bumptech.glide.resize.load;

import android.graphics.Bitmap;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;
import com.bumptech.glide.resize.BitmapLoad;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPool;

public class ResourceBitmapLoad<T> implements BitmapLoad {
    private final String id;
    private final ResourceFetcher<T> fetcher;
    private final BitmapDecoder<T> decoder;
    private final int width;
    private final int height;
    private final Transformation transformation;

    public ResourceBitmapLoad(String modelId, ResourceFetcher<T> fetcher, BitmapDecoder<T> decoder, int width,
            int height) {
        this(modelId, fetcher, decoder, width, height, Transformation.NONE);
    }

    public ResourceBitmapLoad(String modelId, ResourceFetcher<T> fetcher, BitmapDecoder<T> decoder, int width,
            int height, Transformation transformation) {
        this.fetcher = fetcher;
        this.decoder = decoder;
        this.width = width;
        this.height = height;
        this.transformation = transformation;

        this.id = modelId + decoder.getId() + width + height;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void cancel() {
        if (fetcher != null) {
            fetcher.cancel();
        }
    }

    @Override
    public Bitmap load(BitmapPool bitmapPool) throws Exception {
        Bitmap original = null;
        if (fetcher == null || decoder == null) {
            return original;
        }

        T resource = fetcher.loadResource();
        if (resource != null) {
            original = decoder.decode(resource, bitmapPool, width, height);
        }
        Bitmap transformed = original;
        if (original != null) {
            transformed = transformation.transform(original, bitmapPool, width, height);
            if (transformed != null && transformed != original) {
                bitmapPool.put(original);
            }
        }
        return transformed;
    }
}
