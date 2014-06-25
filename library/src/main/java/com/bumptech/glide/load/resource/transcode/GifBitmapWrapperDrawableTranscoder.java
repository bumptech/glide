package com.bumptech.glide.load.resource.transcode;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.gif.GifData;
import com.bumptech.glide.load.resource.gifbitmap.GifBitmapWrapper;

public class GifBitmapWrapperDrawableTranscoder implements ResourceTranscoder<GifBitmapWrapper, Drawable> {
    private final ResourceTranscoder<Bitmap, ? extends Drawable> bitmapDrawableResourceTranscoder;
    private final ResourceTranscoder<GifData, ? extends Drawable> gifDrawableResourceTranscoder;

    public GifBitmapWrapperDrawableTranscoder(ResourceTranscoder<Bitmap, ? extends Drawable> bitmapDrawableResourceTranscoder,
            ResourceTranscoder<GifData, ? extends Drawable> gifDrawableResourceTranscoder) {
        this.bitmapDrawableResourceTranscoder = bitmapDrawableResourceTranscoder;
        this.gifDrawableResourceTranscoder = gifDrawableResourceTranscoder;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Resource<Drawable> transcode(Resource<GifBitmapWrapper> toTranscode) {
        GifBitmapWrapper gifBitmap = toTranscode.get();
        Resource<Bitmap> bitmapResource = gifBitmap.getBitmapResource();

        final Resource<? extends Drawable> result;
        if (bitmapResource != null) {
            result = bitmapDrawableResourceTranscoder.transcode(bitmapResource);
        } else {
            result = gifDrawableResourceTranscoder.transcode(gifBitmap.getGifResource());
        }
        // This is unchecked but always safe, anything that extends a Drawable can be safely cast to a Drawable.
        return (Resource<Drawable>) result;
    }

    @Override
    public String getId() {
        return "GifBitmapWrapperDrawableTranscoder.com.bumptech.glide.load.resource.transcode";
    }
}
