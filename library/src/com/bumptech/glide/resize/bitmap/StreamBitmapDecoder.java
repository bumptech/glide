package com.bumptech.glide.resize.bitmap;

import android.graphics.Bitmap;
import com.bumptech.glide.resize.Resource;
import com.bumptech.glide.resize.ResourceDecoder;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPool;
import com.bumptech.glide.resize.load.DecodeFormat;
import com.bumptech.glide.resize.load.Downsampler;

import java.io.InputStream;

//TODO(actually fill out this stub)
public class StreamBitmapDecoder implements ResourceDecoder<InputStream, Bitmap> {
    private final Downsampler downsampler;
    private BitmapPool bitmapPool;
    private DecodeFormat decodeFormat;

    public StreamBitmapDecoder(BitmapPool bitmapPool) {
        this(Downsampler.AT_LEAST, bitmapPool, DecodeFormat.PREFER_RGB_565);
    }

    public StreamBitmapDecoder(Downsampler downsampler, BitmapPool bitmapPool, DecodeFormat decodeFormat) {
        this.downsampler = downsampler;
        this.bitmapPool = bitmapPool;
        this.decodeFormat = decodeFormat;
    }

    @Override
    public Resource<Bitmap> decode(InputStream source, int width, int height) {
        Bitmap bitmap = downsampler.decode(source, bitmapPool, width, height, decodeFormat);
        if (bitmap == null) {
            return null;
        } else {
            return new BitmapResource(bitmap, bitmapPool);
        }
    }
}
