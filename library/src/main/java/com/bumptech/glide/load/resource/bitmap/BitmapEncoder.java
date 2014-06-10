package com.bumptech.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import com.bumptech.glide.Resource;
import com.bumptech.glide.load.ResourceEncoder;

import java.io.OutputStream;

public class BitmapEncoder implements ResourceEncoder<Bitmap> {
    private Bitmap.CompressFormat compressFormat;
    private int quality;

    public BitmapEncoder() {
        this(null, 70);
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
        return "BitmapEncoder.com.bumptech.glide.load.data.bitmap";
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
