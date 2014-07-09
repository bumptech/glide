package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.ResourceEncoder;

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
    private Bitmap.CompressFormat compressFormat;
    private int quality;

    public BitmapEncoder() {
        this(null, 75);
    }

    public BitmapEncoder(Bitmap.CompressFormat compressFormat, int quality) {
        this.compressFormat = compressFormat;
        this.quality = quality;
    }

    @Override
    public boolean encode(Resource<Bitmap> resource, OutputStream os) {
        final Bitmap bitmap = resource.get();
        bitmap.compress(getFormat(bitmap), quality, os);
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
