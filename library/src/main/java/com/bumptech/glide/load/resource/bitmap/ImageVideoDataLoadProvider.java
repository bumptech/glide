package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import android.os.ParcelFileDescriptor;

import com.bumptech.glide.DataLoadProvider;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.model.ImageVideoWrapper;
import com.bumptech.glide.load.model.ImageVideoWrapperEncoder;
import com.bumptech.glide.load.model.NullEncoder;
import com.bumptech.glide.load.model.StreamEncoder;
import com.bumptech.glide.load.resource.FileToStreamDecoder;

import java.io.File;

public class ImageVideoDataLoadProvider implements DataLoadProvider<ImageVideoWrapper, Bitmap> {
    private final ImageVideoBitmapDecoder sourceDecoder;
    private final ResourceDecoder<File, Bitmap> cacheDecoder;
    private final BitmapEncoder encoder;
    private final ImageVideoWrapperEncoder sourceEncoder;
    private final StreamBitmapDecoder streamDecoder;

    public ImageVideoDataLoadProvider(BitmapPool bitmapPool) {
        encoder = new BitmapEncoder();
        Encoder<ParcelFileDescriptor> fileDescriptorEncoder = NullEncoder.get();
        sourceEncoder = new ImageVideoWrapperEncoder(new StreamEncoder(), fileDescriptorEncoder);
        streamDecoder = new StreamBitmapDecoder(bitmapPool);
        cacheDecoder = new FileToStreamDecoder<Bitmap>(streamDecoder);
        sourceDecoder = new ImageVideoBitmapDecoder(streamDecoder, new FileDescriptorBitmapDecoder(bitmapPool));
    }

    @Override
    public ResourceDecoder<File, Bitmap> getCacheDecoder() {
        return cacheDecoder;
    }

    @Override
    public ResourceDecoder<ImageVideoWrapper, Bitmap> getSourceDecoder() {
        return sourceDecoder;
    }

    @Override
    public Encoder<ImageVideoWrapper> getSourceEncoder() {
        return sourceEncoder;
    }

    @Override
    public ResourceEncoder<Bitmap> getEncoder() {
        return encoder;
    }
}
