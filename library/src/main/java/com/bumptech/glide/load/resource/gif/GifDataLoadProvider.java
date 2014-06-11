package com.bumptech.glide.load.resource.gif;

import android.content.Context;
import com.bumptech.glide.DataLoadProvider;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

import java.io.InputStream;

public class GifDataLoadProvider implements DataLoadProvider<InputStream, GifData> {
    private final GifResourceDecoder decoder;
    private final GifResourceEncoder encoder;

    public GifDataLoadProvider(Context context, BitmapPool bitmapPool) {
        decoder = new GifResourceDecoder(context, bitmapPool);
        encoder = new GifResourceEncoder();
    }

    @Override
    public ResourceDecoder<InputStream, GifData> getCacheDecoder() {
        return decoder;
    }

    @Override
    public ResourceDecoder<InputStream, GifData> getSourceDecoder() {
        return decoder;
    }

    @Override
    public ResourceEncoder<GifData> getEncoder() {
        return encoder;
    }
}
