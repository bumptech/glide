package com.bumptech.glide.load.resource.gif;

import android.content.Context;
import android.graphics.Bitmap;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.gif.decoder.GifDecoder;
import com.bumptech.glide.load.resource.gif.decoder.GifHeader;

import java.util.ArrayList;
import java.util.List;

public class GifData {
    private final Context context;
    private final BitmapPool bitmapPool;
    private final GifHeader header;
    private final byte[] data;
    private String gifId;
    private final int targetWidth;
    private final int targetHeight;
    private final List<GifDrawable> drawables = new ArrayList<GifDrawable>();
    private Transformation<Bitmap> frameTransformation;

    public GifData(Context context, BitmapPool bitmapPool, String gifId, GifHeader header, byte[] data,
            int targetWidth, int targetHeight) {
        this.context = context;
        this.bitmapPool = bitmapPool;
        this.header = header;
        this.data = data;
        this.gifId = gifId;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
    }

    @SuppressWarnings("unchecked")
    public Transformation<Bitmap> getFrameTransformation() {
        return frameTransformation != null ? frameTransformation : Transformation.NONE;
    }

    public void setFrameTransformation(Transformation<Bitmap> transformation) {
        this.frameTransformation = transformation;
    }

    public int getByteSize() {
        return data.length;
    }

    public byte[] getData() {
        return data;
    }

    public GifDrawable getDrawable() {
        GifDecoder gifDecoder = new GifDecoder(bitmapPool);
        gifDecoder.setData(gifId, header, data);
        GifFrameManager frameManager = new GifFrameManager(context, gifDecoder, getFrameTransformation(),
                targetWidth, targetHeight);

        GifDrawable result = new GifDrawable(gifDecoder, frameManager);
        drawables.add(result);
        return result;
    }

    public void recycle() {
        for (GifDrawable drawable : drawables) {
            drawable.stop();
            drawable.recycle();
        }
    }
}
