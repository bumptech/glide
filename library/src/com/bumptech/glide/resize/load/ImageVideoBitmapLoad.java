package com.bumptech.glide.resize.load;

import android.graphics.Bitmap;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;
import com.bumptech.glide.resize.BitmapLoad;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPool;

/**
 * A base {@link BitmapLoad} that composes {@link ResourceFetcher} and {@link BitmapDecoder} to decode a
 * bitmap from either an image or a video.
 */
public class ImageVideoBitmapLoad implements BitmapLoad {
    private final String id;
    private final int width;
    private final int height;
    private final BitmapLoad imageLoad;
    private final BitmapLoad videoLoad;
    private final Transformation transformation;

    public ImageVideoBitmapLoad(BitmapLoad imageLoad, BitmapLoad videoLoad, int width,
            int height, Transformation transformation) {
        this.imageLoad = imageLoad;
        this.videoLoad = videoLoad;
        this.transformation = transformation;
        this.width = width;
        this.height = height;

        StringBuilder idBuilder = new StringBuilder();
        if (imageLoad != null) {
            idBuilder.append(imageLoad.getId());
        }
        if (videoLoad != null) {
            idBuilder.append(videoLoad.getId());
        }
        this.id = idBuilder.append(transformation.getId()).toString();
    }

    public void cancel() {
        if (imageLoad != null) {
            imageLoad.cancel();
        }
        if (videoLoad != null) {
            videoLoad.cancel();
        }
    }

    @Override
    public Bitmap load(BitmapPool bitmapPool) throws Exception {
        Bitmap original = null;
        if (imageLoad != null) {
            original = imageLoad.load(bitmapPool);
        }
        if (original == null && videoLoad != null) {
            original = videoLoad.load(bitmapPool);
        }

        if (original == null) {
            return null;
        }

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
