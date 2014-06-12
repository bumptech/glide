package com.bumptech.glide.load.resource.gifbitmap;

import android.graphics.Bitmap;
import com.bumptech.glide.DataLoadProvider;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.model.ImageVideoWrapper;
import com.bumptech.glide.load.resource.gif.GifData;

import java.io.InputStream;

public class ImageVideoGifDataLoadProvider implements DataLoadProvider<ImageVideoWrapper, GifBitmapWrapper> {
    private final GifBitmapWrapperStreamResourceDecoder cacheDecoder;
    private final GifBitmapWrapperResourceDecoder sourceDecoder;
    private final GifBitmapWrapperResourceEncoder encoder;

    public ImageVideoGifDataLoadProvider(DataLoadProvider<ImageVideoWrapper, Bitmap> bitmapProvider,
            DataLoadProvider<InputStream, GifData> gifProvider) {
        cacheDecoder = new GifBitmapWrapperStreamResourceDecoder(new GifBitmapWrapperResourceDecoder(
                bitmapProvider.getSourceDecoder(),
                gifProvider.getCacheDecoder()));
        sourceDecoder = new GifBitmapWrapperResourceDecoder(
                bitmapProvider.getSourceDecoder(),
                gifProvider.getSourceDecoder());
        encoder = new GifBitmapWrapperResourceEncoder(bitmapProvider.getEncoder(), gifProvider.getEncoder());
    }

    @Override
    public ResourceDecoder<InputStream, GifBitmapWrapper> getCacheDecoder() {
        return cacheDecoder;
    }

    @Override
    public ResourceDecoder<ImageVideoWrapper, GifBitmapWrapper> getSourceDecoder() {
        return sourceDecoder;
    }

    @Override
    public ResourceEncoder<GifBitmapWrapper> getEncoder() {
        return encoder;
    }
}
