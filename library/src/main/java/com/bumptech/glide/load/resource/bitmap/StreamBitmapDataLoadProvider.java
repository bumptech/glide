package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import com.bumptech.glide.DataLoadProvider;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

import java.io.InputStream;

public class StreamBitmapDataLoadProvider implements DataLoadProvider<InputStream, Bitmap> {
    private final StreamBitmapDecoder decoder;
    private final BitmapEncoder encoder;

    public StreamBitmapDataLoadProvider(BitmapPool bitmapPool) {
        decoder = new StreamBitmapDecoder(bitmapPool);
        encoder = new BitmapEncoder();
    }

    @Override
    public ResourceDecoder<InputStream, Bitmap> getCacheDecoder() {
        return decoder;
    }

    @Override
    public ResourceDecoder<InputStream, Bitmap> getSourceDecoder() {
        return decoder;
    }

    @Override
    public ResourceEncoder<Bitmap> getEncoder() {
        return encoder;
    }
}
