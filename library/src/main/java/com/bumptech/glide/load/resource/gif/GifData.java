package com.bumptech.glide.load.resource.gif;

import android.content.Context;
import android.graphics.Bitmap;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.gifdecoder.GifHeader;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.UnitTransformation;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

import java.util.ArrayList;
import java.util.List;

public class GifData {
    private final Context context;
    private final GifHeader header;
    private final byte[] data;
    private final GifDecoderBitmapProvider bitmapProvider;
    private final String gifId;
    private final int targetWidth;
    private final int targetHeight;
    private final List<GifDrawable> drawables = new ArrayList<GifDrawable>();
    private Transformation<Bitmap> frameTransformation;

    public GifData(Context context, BitmapPool bitmapPool, String gifId, GifHeader header, byte[] data,
            int targetWidth, int targetHeight) {
        this.context = context;
        this.header = header;
        this.data = data;
        this.gifId = gifId;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        bitmapProvider = new GifDecoderBitmapProvider(bitmapPool);
    }

    @SuppressWarnings("unchecked")
    public Transformation<Bitmap> getFrameTransformation() {
        if (frameTransformation != null) {
            return frameTransformation;
        } else {
            return UnitTransformation.get();
        }
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
        GifDecoder gifDecoder = new GifDecoder(bitmapProvider);
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

    private static class GifDecoderBitmapProvider implements GifDecoder.BitmapProvider {
        private BitmapPool bitmapPool;

        public GifDecoderBitmapProvider(BitmapPool bitmapPool) {
            this.bitmapPool = bitmapPool;
        }

        @Override
        public Bitmap obtain(int width, int height, Bitmap.Config config) {
            return bitmapPool.get(width, height, config);
        }
    }
}
