package com.bumptech.glide.integration.palette;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.graphics.Palette;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;

/**
 * A {@link com.bumptech.glide.load.resource.transcode.ResourceTranscoder} for generating
 * {@link android.support.v7.graphics.Palette}s from {@link android.graphics.Bitmap}s in the background.
 */
public class PaletteBitmapTranscoder implements ResourceTranscoder<Bitmap, PaletteBitmap> {

    private final BitmapPool bitmapPool;

    public PaletteBitmapTranscoder(Context context) {
        bitmapPool = Glide.get(context).getBitmapPool();
    }

    @Override
    public Resource<PaletteBitmap> transcode(Resource<Bitmap> toTranscode) {
        Palette palette = Palette.generate(toTranscode.get());
        PaletteBitmap result = new PaletteBitmap(toTranscode.get(), palette);
        return new PaletteBitmapResource(result, bitmapPool);
    }

    @Override
    public String getId() {
        return "";
    }
}
