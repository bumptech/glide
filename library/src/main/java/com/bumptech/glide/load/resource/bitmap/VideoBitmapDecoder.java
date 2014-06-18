package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.ParcelFileDescriptor;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.DecodeFormat;

import java.io.IOException;

public class VideoBitmapDecoder implements BitmapDecoder<ParcelFileDescriptor> {
    private static final DefaultFactory DEFAULT_FACTORY = new DefaultFactory();
    private MediaMetadataRetrieverFactory factory;

    interface MediaMetadataRetrieverFactory {
        public MediaMetadataRetriever build();
    }

    public VideoBitmapDecoder() {
        this(DEFAULT_FACTORY);
    }


    VideoBitmapDecoder(MediaMetadataRetrieverFactory factory) {
        this.factory = factory;
    }

    @Override
    public Bitmap decode(ParcelFileDescriptor resource, BitmapPool bitmapPool, int outWidth, int outHeight,
            DecodeFormat decodeFormat)
            throws IOException {
        MediaMetadataRetriever mediaMetadataRetriever = factory.build();
        mediaMetadataRetriever.setDataSource(resource.getFileDescriptor());
        Bitmap result = mediaMetadataRetriever.getFrameAtTime();
        mediaMetadataRetriever.release();
        resource.close();
        return result;
    }

    @Override
    public String getId() {
        return "VideoBitmapDecoder.com.bumptech.glide.load.resource.bitmap";
    }

    private static class DefaultFactory implements MediaMetadataRetrieverFactory {
        @Override
        public MediaMetadataRetriever build() {
            return new MediaMetadataRetriever();
        }
    }
}
