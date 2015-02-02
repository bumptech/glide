package com.bumptech.glide.load.resource.bitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.ParcelFileDescriptor;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

import java.io.IOException;
import java.util.Map;

/**
 * An {@link com.bumptech.glide.load.ResourceDecoder} for decoding {@link android.graphics.Bitmap}s from
 * {@link android.os.ParcelFileDescriptor} data.
 */
public class FileDescriptorBitmapDecoder implements ResourceDecoder<ParcelFileDescriptor, Bitmap> {
    private final VideoBitmapDecoder bitmapDecoder;
    private final BitmapPool bitmapPool;

    public FileDescriptorBitmapDecoder(Context context) {
        this(Glide.get(context).getBitmapPool());
    }

    public FileDescriptorBitmapDecoder(BitmapPool bitmapPool) {
        this(new VideoBitmapDecoder(), bitmapPool);
    }

    public FileDescriptorBitmapDecoder(VideoBitmapDecoder bitmapDecoder, BitmapPool bitmapPool) {
        this.bitmapDecoder = bitmapDecoder;
        this.bitmapPool = bitmapPool;
    }

    @Override
    public boolean handles(ParcelFileDescriptor source) {
        return bitmapDecoder.handles(source);
    }

    @Override
    public Resource<Bitmap> decode(ParcelFileDescriptor source, int width, int height, Map<String, Object> options)
            throws IOException {
        Bitmap bitmap = bitmapDecoder.decode(source, bitmapPool, width, height, options);
        return BitmapResource.obtain(bitmap, bitmapPool);
    }
}
