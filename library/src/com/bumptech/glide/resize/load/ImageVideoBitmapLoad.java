package com.bumptech.glide.resize.load;

import android.graphics.Bitmap;
import android.util.Log;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;
import com.bumptech.glide.resize.BitmapLoad;
import com.bumptech.glide.resize.Metadata;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPool;

/**
 * A base {@link BitmapLoad} that composes {@link ResourceFetcher} and {@link BitmapDecoder} to decode a
 * bitmap from either an image or a video.
 */
public class ImageVideoBitmapLoad implements BitmapLoad {
    private static final String TAG = "IVBL";

    private final String id;
    private final int width;
    private final int height;
    private final BitmapLoad imageLoad;
    private final BitmapLoad videoLoad;
    private final Transformation transformation;
    private Metadata metadata;

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
    public Metadata getMetadata() {
        return metadata;
    }

    @Override
    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
        if (imageLoad != null) {
            imageLoad.setMetadata(metadata);
        }
        if (videoLoad != null) {
            videoLoad.setMetadata(metadata);
        }
    }

    @Override
    public Bitmap load(BitmapPool bitmapPool) throws Exception {
        long now = System.currentTimeMillis();
        Bitmap original = null;
        if (imageLoad != null) {
            original = imageLoad.load(bitmapPool);
        }
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "loaded image in " + (System.currentTimeMillis() - now));
            now = System.currentTimeMillis();
        }
        if (original == null && videoLoad != null) {
            original = videoLoad.load(bitmapPool);
        }
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "loaded video in " + (System.currentTimeMillis() - now));
            now = System.currentTimeMillis();
        }

        if (original == null) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Unable to decode either image or video");
            }
            return null;
        }

        Bitmap transformed = transformation.transform(original, bitmapPool, width, height);
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "transformed in " + (System.currentTimeMillis() - now));
        }
        if (original != transformed) {
            bitmapPool.put(original);
        }
        return transformed;
    }

    public String getId() {
        return id;
    }
}
