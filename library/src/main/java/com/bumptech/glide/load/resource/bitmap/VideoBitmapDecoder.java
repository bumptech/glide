package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.ParcelFileDescriptor;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

import java.io.IOException;
import java.util.Map;

/**
 * An {@link com.bumptech.glide.load.resource.bitmap.BitmapDecoder} that can decode a thumbnail frame
 * {@link Bitmap} from a {@link android.os.ParcelFileDescriptor} containing a video.
 *
 * @see android.media.MediaMetadataRetriever
 */
public class VideoBitmapDecoder implements BitmapDecoder<ParcelFileDescriptor> {
    public static final String KEY_TARGET_FRAME =
            "com.bumtpech.glide.load.resource.bitmap.VideoBitmapDecode.TargetFrame";
    /**
     * A constant indicating we should use whatever frame we consider best, frequently not the first frame.
     */
    public static final int DEFAULT_FRAME = -1;

    private static final MediaMetadataRetrieverFactory DEFAULT_FACTORY =  new MediaMetadataRetrieverFactory();

    private final MediaMetadataRetrieverFactory factory;

    public VideoBitmapDecoder() {
        this(DEFAULT_FACTORY);
    }

    VideoBitmapDecoder(MediaMetadataRetrieverFactory factory) {
        this.factory = factory;
    }

    @Override
    public boolean handles(ParcelFileDescriptor data) {
        MediaMetadataRetriever retriever = factory.build();
        try {
            retriever.setDataSource(data.getFileDescriptor());
            return true;
        } catch (RuntimeException e) {
            // Throws a generic runtime exception when given invalid data.
            return false;
        } finally {
            retriever.release();
        }
    }

    @Override
    public Bitmap decode(ParcelFileDescriptor resource, BitmapPool bitmapPool, int outWidth, int outHeight,
            Map<String, Object> options) throws IOException {
        int frame = options.containsKey(KEY_TARGET_FRAME) ? (Integer) options.get(KEY_TARGET_FRAME) : DEFAULT_FRAME;
        if (frame < 0 && frame != DEFAULT_FRAME) {
            throw new IllegalArgumentException("Requested frame must be non-negative, or DEFAULT_FRAME, given: " + frame);
        }

        MediaMetadataRetriever mediaMetadataRetriever = factory.build();
        mediaMetadataRetriever.setDataSource(resource.getFileDescriptor());
        final Bitmap result;
        if (frame == DEFAULT_FRAME) {
            result = mediaMetadataRetriever.getFrameAtTime();
        } else {
            result = mediaMetadataRetriever.getFrameAtTime(frame);
        }
        mediaMetadataRetriever.release();
        resource.close();
        return result;
    }

    @Override
    public String getId() {
        return "VideoBitmapDecoder.com.bumptech.glide.load.resource.bitmap";
    }

    // Visible for testing.
    static class MediaMetadataRetrieverFactory {
        public MediaMetadataRetriever build() {
            return new MediaMetadataRetriever();
        }
    }
}
