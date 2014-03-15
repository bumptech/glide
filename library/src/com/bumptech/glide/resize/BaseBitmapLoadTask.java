package com.bumptech.glide.resize;

import android.graphics.Bitmap;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPool;
import com.bumptech.glide.resize.load.BitmapDecoder;
import com.bumptech.glide.resize.load.Transformation;

/**
 * A base {@link BitmapLoadTask} that composes {@link ResourceFetcher} and {@link BitmapDecoder} to decode a
 * bitmap.
 *
 * @param <T> The type of the resource returned by the {@link ResourceFetcher} and used by the
 *          {@link BitmapDecoder} to BitmapResourceLoader the bitmap.
 */
public class BaseBitmapLoadTask<T> implements BitmapLoadTask {
    private final String id;
    private final int width;
    private final int height;
    private final Transformation transformation;
    private ResourceFetcher<T> loader;
    private BitmapDecoder<T> decoder;

    public BaseBitmapLoadTask(ResourceFetcher<T> loader, BitmapDecoder<T> decoder, Transformation transformation,
            int width, int height) {
        this.loader = loader;
        this.decoder = decoder;
        this.transformation = transformation;
        this.width = width;
        this.height = height;
        this.id = loader.getId() + decoder.getId() + transformation.getId() + width + height;
    }

    public void cancel() {
        loader.cancel();
    }

    @Override
    public Bitmap load(BitmapPool bitmapPool) throws Exception {
        T resource = loader.loadResource();
        if (resource == null) {
            throw new IllegalStateException("Cannot decode null resource");
        }
        Bitmap original = decoder.decode(resource, bitmapPool, width, height);
        Bitmap transformed = transformation.transform(original, bitmapPool, width, height);
        if (original != transformed) {
            bitmapPool.put(original);
        }
        return transformed;
    }

    public String getId() {
        return id;
    }
}
