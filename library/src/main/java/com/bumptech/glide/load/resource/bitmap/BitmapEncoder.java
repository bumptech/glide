package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import android.util.Log;

import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.util.LogTime;
import com.bumptech.glide.util.Util;

import java.io.OutputStream;

/**
 * An {@link com.bumptech.glide.load.ResourceEncoder} that writes {@link android.graphics.Bitmap}s to
 * {@link java.io.OutputStream}s.
 *
 * <p>
 *     {@link android.graphics.Bitmap}s that return true from {@link android.graphics.Bitmap#hasAlpha()}} are written
 *     using {@link android.graphics.Bitmap.CompressFormat#PNG} to preserve alpha and all other bitmaps are written
 *     using {@link android.graphics.Bitmap.CompressFormat#JPEG}.
 * </p>
 *
 * @see android.graphics.Bitmap#compress(android.graphics.Bitmap.CompressFormat, int, java.io.OutputStream)
 */
public class BitmapEncoder implements ResourceEncoder<Bitmap> {
    private static final String TAG = "BitmapEncoder";
    private static final int DEFAULT_COMPRESSION_QUALITY = 90;
    private Bitmap.CompressFormat compressFormat;
    private int quality;

    public BitmapEncoder() {
        this(null, DEFAULT_COMPRESSION_QUALITY);
    }

    public BitmapEncoder(Bitmap.CompressFormat compressFormat, int quality) {
        this.compressFormat = compressFormat;
        this.quality = quality;
    }

    @Override
    public boolean encode(Resource<Bitmap> resource, OutputStream os) {
        final Bitmap bitmap = resource.get();

        long start = LogTime.getLogTime();
        Bitmap.CompressFormat format = getFormat(bitmap);
        bitmap.compress(format, quality, os);
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Compressed with type: " + format + " of size " + Util.getBitmapByteSize(bitmap) + " in "
                    + LogTime.getElapsedMillis(start));
        }
        return true;
    }

    @Override
    public String getId() {
        return "BitmapEncoder.com.bumptech.glide.load.resource.bitmap";
    }

    private Bitmap.CompressFormat getFormat(Bitmap bitmap) {
        if (compressFormat != null) {
            return compressFormat;
        } else if (bitmap.hasAlpha()) {
            return Bitmap.CompressFormat.PNG;
        } else {
            return Bitmap.CompressFormat.JPEG;
        }
    }

}
