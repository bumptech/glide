package com.bumptech.glide.resize.load;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.ParcelFileDescriptor;
import com.bumptech.glide.resize.bitmap_recycle.BitmapPool;

import java.io.IOException;

public class VideoBitmapDecoder implements BitmapDecoder<ParcelFileDescriptor> {
    private static final String ID = "VideoBitmapDecoder";
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
        return ID;
    }
}
