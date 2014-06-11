package com.bumptech.glide.load.resource.gifbitmap;

import android.graphics.Bitmap;
import com.bumptech.glide.DataLoadProvider;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.model.ImageVideoWrapper;
import com.bumptech.glide.load.resource.gif.GifData;

import java.io.InputStream;

public class ImageVideoGifDataLoadProvider implements DataLoadProvider<ImageVideoWrapper, GifBitmap> {
    private final GifBitmapStreamResourceDecoder cacheDecoder;
    private final GifBitmapResourceDecoder sourceDecoder;
    private final GifBitmapResourceEncoder encoder;

    public ImageVideoGifDataLoadProvider(DataLoadProvider<ImageVideoWrapper, Bitmap> bitmapProvider,
            DataLoadProvider<InputStream, GifData> gifProvider) {
        cacheDecoder = new GifBitmapStreamResourceDecoder(new GifBitmapResourceDecoder(
                bitmapProvider.getSourceDecoder(),
                gifProvider.getCacheDecoder()));
        sourceDecoder = new GifBitmapResourceDecoder(
                bitmapProvider.getSourceDecoder(),
                gifProvider.getSourceDecoder());
        encoder = new GifBitmapResourceEncoder(bitmapProvider.getEncoder(), gifProvider.getEncoder());
    }

    @Override
    public ResourceDecoder<InputStream, GifBitmap> getCacheDecoder() {
        return cacheDecoder;
    }

    @Override
    public ResourceDecoder<ImageVideoWrapper, GifBitmap> getSourceDecoder() {
        return sourceDecoder;
    }

    @Override
    public ResourceEncoder<GifBitmap> getEncoder() {
        return encoder;
    }
}
