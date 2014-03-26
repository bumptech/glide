package com.bumptech.glide.resize;

import android.graphics.Bitmap;
import com.bumptech.glide.loader.bitmap.resource.ResourceFetcher;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPool;
import com.bumptech.glide.resize.load.BitmapDecoder;
import com.bumptech.glide.resize.load.Transformation;

/**
 * A base {@link BitmapLoad} that composes {@link ResourceFetcher} and {@link BitmapDecoder} to decode a
 * bitmap from either an image or a video.
 *
 * @param <T> The type of the resource returned by the image {@link ResourceFetcher} and used by the
 *          image {@link BitmapDecoder} to decode the bitmap.
 * @param <Y> The type of the resource returned by the video {@link ResourceFetcher} and used by the
 *           video {@link BitmapDecoder} to decode the bitmap.
 */
public class ImageVideoBitmapLoad<T, Y> implements BitmapLoad {
    private final String id;
    private final int width;
    private final int height;
    private final ResourceFetcher<Y> videoFetcher;
    private final BitmapDecoder<Y> videoDecoder;
    private final Transformation transformation;
    private ResourceFetcher<T> imageFetcher;
    private BitmapDecoder<T> imageDecoder;

    public ImageVideoBitmapLoad(ResourceFetcher<T> imageFetcher, BitmapDecoder<T> imageDecoder,
            ResourceFetcher<Y> videoFetcher, BitmapDecoder<Y> videoDecoder, Transformation transformation,
            int width, int height) {
        this.imageFetcher = imageFetcher;
        this.imageDecoder = imageDecoder;
        this.videoFetcher = videoFetcher;
        this.videoDecoder = videoDecoder;
        this.transformation = transformation;
        this.width = width;
        this.height = height;

        StringBuilder idBuilder = new StringBuilder();
        if (imageFetcher != null && imageDecoder != null) {
            idBuilder.append(imageFetcher.getId());
            idBuilder.append(imageDecoder.getId());
        }
        if (videoFetcher != null && videoDecoder != null) {
            idBuilder.append(videoFetcher.getId());
            idBuilder.append(videoDecoder.getId());
        }
        this.id = idBuilder.append(transformation.getId()).append(width).append(height).toString();
    }

    public void cancel() {
        if (imageFetcher != null) {
            imageFetcher.cancel();
        }
        if (videoFetcher != null) {
            videoFetcher.cancel();
        }
    }

    @Override
    public Bitmap load(BitmapPool bitmapPool) throws Exception {
        Bitmap original = loadOriginal(bitmapPool);
        if (original == null) {
            return null;
        }

        Bitmap transformed = transformation.transform(original, bitmapPool, width, height);
        if (original != transformed) {
            bitmapPool.put(original);
        }
        return transformed;
    }

    private Bitmap loadOriginal(BitmapPool bitmapPool) throws Exception {
        Bitmap result = loadOriginal(imageFetcher, imageDecoder, bitmapPool);
        if (result == null) {
            result = loadOriginal(videoFetcher, videoDecoder, bitmapPool);
        }

        return result;
    }

    private <X> Bitmap loadOriginal(ResourceFetcher<X> fetcher, BitmapDecoder<X> decoder, BitmapPool bitmapPool)
            throws Exception {
        if (fetcher == null || decoder == null) {
            return  null;
        }

        X resource = fetcher.loadResource();
        if (resource == null) {
            return  null;
        }

        return decoder.decode(resource, bitmapPool, width, height);
    }

    public String getId() {
        return id;
    }
}
