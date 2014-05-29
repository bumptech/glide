package com.bumptech.glide.resize;

import android.graphics.Bitmap;
import com.bumptech.glide.resize.bitmap.BitmapEncoder;
import com.bumptech.glide.resize.bitmap.StreamBitmapDecoder;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPool;

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
