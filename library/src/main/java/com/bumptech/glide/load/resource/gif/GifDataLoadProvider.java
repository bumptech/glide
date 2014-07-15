package com.bumptech.glide.load.resource.gif;

import android.content.Context;

import com.bumptech.glide.provider.DataLoadProvider;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.model.StreamEncoder;
import com.bumptech.glide.load.resource.file.FileToStreamDecoder;

import java.io.File;
import java.io.InputStream;

/**
 * An {@link com.bumptech.glide.provider.DataLoadProvider} that loads an {@link java.io.InputStream} into
 * {@link com.bumptech.glide.load.resource.gif.GifData} that can be used to display an animated GIF.
 */
public class GifDataLoadProvider implements DataLoadProvider<InputStream, GifData> {
    private final GifResourceDecoder decoder;
    private final GifResourceEncoder encoder;
    private final StreamEncoder sourceEncoder;
    private final FileToStreamDecoder<GifData> cacheDecoder;

    public GifDataLoadProvider(Context context, BitmapPool bitmapPool) {
        decoder = new GifResourceDecoder(context, bitmapPool);
        cacheDecoder = new FileToStreamDecoder<GifData>(decoder);
        encoder = new GifResourceEncoder();
        sourceEncoder = new StreamEncoder();
    }

    @Override
    public ResourceDecoder<File, GifData> getCacheDecoder() {
        return cacheDecoder;
    }

    @Override
    public ResourceDecoder<InputStream, GifData> getSourceDecoder() {
        return decoder;
    }

    @Override
    public Encoder<InputStream> getSourceEncoder() {
        return sourceEncoder;
    }

    @Override
    public ResourceEncoder<GifData> getEncoder() {
        return encoder;
    }
}
