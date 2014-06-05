package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.ParcelFileDescriptor;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.DecodeFormat;

import java.io.IOException;

public class VideoBitmapDecoder implements BitmapDecoder<ParcelFileDescriptor> {
    @Override
    public Bitmap decode(ParcelFileDescriptor resource, BitmapPool bitmapPool, int outWidth, int outHeight,
            DecodeFormat decodeFormat)
            throws IOException {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(resource.getFileDescriptor());
        Bitmap result = mediaMetadataRetriever.getFrameAtTime();
        mediaMetadataRetriever.release();
        resource.close();
        return result;
    }

    @Override
    public String getId() {
        return "VideoBitmapDecoder.com.bumptech.glide.load.data.bitmap";
    }
}
