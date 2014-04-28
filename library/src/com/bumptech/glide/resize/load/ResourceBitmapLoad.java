package com.bumptech.glide.resize.load;

import android.graphics.Bitmap;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;
import com.bumptech.glide.resize.Metadata;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPool;

public class ResourceBitmapLoad<T> implements BitmapLoad {
    private final String id;
    private final ResourceFetcher<T> fetcher;
    private final BitmapDecoder<T> decoder;
    private final int width;
    private final int height;
    private final Transformation transformation;
    private final DecodeFormat decodeFormat;
    private Metadata metadata;

    public ResourceBitmapLoad(String modelId, ResourceFetcher<T> fetcher, BitmapDecoder<T> decoder, int width,
            int height, Transformation transformation, DecodeFormat decodeFormat) {
        this.fetcher = fetcher;
        this.decoder = decoder;
        this.width = width;
        this.height = height;
        this.transformation = transformation;
        this.decodeFormat = decodeFormat;

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
    public Metadata getMetadata() {
        return metadata;
    }

    @Override
    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public Bitmap load(BitmapPool bitmapPool) throws Exception {
        Bitmap original = null;
        if (fetcher == null || decoder == null) {
            return original;
        }

        T resource = fetcher.loadResource(metadata);
        if (resource != null) {
            original = decoder.decode(resource, bitmapPool, width, height, decodeFormat);
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
