package com.bumptech.glide.load.resource.gifbitmap;

import android.graphics.Bitmap;
import android.os.ParcelFileDescriptor;
import com.bumptech.glide.DataLoadProvider;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.model.ImageVideoWrapper;
import com.bumptech.glide.load.model.NullEncoder;
import com.bumptech.glide.load.resource.gif.GifData;

import java.io.InputStream;

public class ImageVideoGifDataLoadProvider implements DataLoadProvider<ImageVideoWrapper, GifBitmapWrapper> {
    private final GifBitmapWrapperStreamResourceDecoder cacheDecoder;
    private final GifBitmapWrapperResourceDecoder sourceDecoder;
    private final GifBitmapWrapperResourceEncoder encoder;
    private final Encoder<ImageVideoWrapper> sourceEncoder;

    public ImageVideoGifDataLoadProvider(DataLoadProvider<ImageVideoWrapper, Bitmap> bitmapProvider,
            DataLoadProvider<InputStream, GifData> gifProvider) {
        cacheDecoder = new GifBitmapWrapperStreamResourceDecoder(new GifBitmapWrapperResourceDecoder(
                bitmapProvider.getSourceDecoder(),
                gifProvider.getCacheDecoder()));
        sourceDecoder = new GifBitmapWrapperResourceDecoder(
                bitmapProvider.getSourceDecoder(),
                gifProvider.getSourceDecoder());
        encoder = new GifBitmapWrapperResourceEncoder(bitmapProvider.getEncoder(), gifProvider.getEncoder());

        Encoder<ParcelFileDescriptor> fileDescriptorEncoder = NullEncoder.get();

        //TODO: what about the gif provider?
        sourceEncoder = bitmapProvider.getSourceEncoder();
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
    public Encoder<ImageVideoWrapper> getSourceEncoder() {
        return sourceEncoder;
    }

    @Override
    public ResourceEncoder<GifBitmapWrapper> getEncoder() {
        return encoder;
    }
}
