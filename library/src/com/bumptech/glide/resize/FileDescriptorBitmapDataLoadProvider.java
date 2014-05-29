package com.bumptech.glide.resize;

import android.graphics.Bitmap;
import android.os.ParcelFileDescriptor;
import com.bumptech.glide.resize.bitmap.BitmapEncoder;
import com.bumptech.glide.resize.bitmap.FileDescriptorBitmapDecoder;
import com.bumptech.glide.resize.bitmap.StreamBitmapDecoder;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPool;

import java.io.InputStream;

public class FileDescriptorBitmapDataLoadProvider implements DataLoadProvider<ParcelFileDescriptor, Bitmap> {
    private final StreamBitmapDecoder cacheDecoder;
    private final FileDescriptorBitmapDecoder sourceDecoder;
    private final BitmapEncoder encoder;

    public FileDescriptorBitmapDataLoadProvider(BitmapPool bitmapPool) {
        cacheDecoder = new StreamBitmapDecoder(bitmapPool);
        sourceDecoder = new FileDescriptorBitmapDecoder(bitmapPool);
        encoder = new BitmapEncoder();
    }

    @Override
    public ResourceDecoder<InputStream, Bitmap> getCacheDecoder() {
        return cacheDecoder;
    }

    @Override
    public ResourceDecoder<ParcelFileDescriptor, Bitmap> getSourceDecoder() {
        return sourceDecoder;
    }

    @Override
    public ResourceEncoder<Bitmap> getEncoder() {
        return encoder;
    }
}
