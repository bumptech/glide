package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import com.bumptech.glide.DataLoadProvider;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.model.ImageVideoWrapper;

import java.io.InputStream;

public class ImageVideoDataLoadProvider implements DataLoadProvider<ImageVideoWrapper, Bitmap> {
    private final ImageVideoBitmapDecoder sourceDecoder;
    private final StreamBitmapDecoder cacheDecoder;
    private final BitmapEncoder encoder;

    public ImageVideoDataLoadProvider(BitmapPool bitmapPool) {
        encoder = new BitmapEncoder();
        cacheDecoder = new StreamBitmapDecoder(bitmapPool);
        sourceDecoder = new ImageVideoBitmapDecoder(cacheDecoder,
                new FileDescriptorBitmapDecoder(bitmapPool));
    }

    @Override
    public ResourceDecoder<InputStream, Bitmap> getCacheDecoder() {
        return cacheDecoder;
    }

    @Override
    public ResourceDecoder<ImageVideoWrapper, Bitmap> getSourceDecoder() {
        return sourceDecoder;
    }

    @Override
    public ResourceEncoder<Bitmap> getEncoder() {
        return encoder;
    }
}
