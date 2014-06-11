package com.bumptech.glide.load.resource.gif;

import android.content.Context;
import android.graphics.Bitmap;
import com.bumptech.glide.load.Transformation;

public class GifData {
    private final Context context;
    private GifDecoder gifDecoder;
    private final Transformation<Bitmap> frameTransformation;
    private GifDrawable drawable;

    public GifData(Context context, GifDecoder gifDecoder, Transformation<Bitmap> frameTransformation) {
        this.context = context;
        this.gifDecoder = gifDecoder;
        this.frameTransformation = frameTransformation;
    }

    public GifDecoder getGifDecoder() {
        return gifDecoder;
    }

    public Transformation<Bitmap> getFrameTransformation() {
        return frameTransformation;
    }

    public int getByteSize() {
        return gifDecoder.getGifByteSize();
    }

    public byte[] getData() {
        return gifDecoder.getData();
    }

    public GifDrawable getDrawable() {
        if (drawable == null) {
            drawable = new GifDrawable(gifDecoder, new GifFrameManager(context, frameTransformation));
        }

        return drawable;
    }

    public void recycle() {
        if (drawable != null) {
            drawable.stop();
            drawable.recycle();
        }
    }
}
