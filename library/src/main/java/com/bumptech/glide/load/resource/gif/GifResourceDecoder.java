package com.bumptech.glide.load.resource.gif;

import android.content.Context;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

import java.io.IOException;
import java.io.InputStream;

public class GifResourceDecoder implements ResourceDecoder<InputStream, GifDrawable> {
    private Context context;
    private BitmapPool bitmapPool;

    public GifResourceDecoder(Context context) {
        this(context, Glide.get(context).getBitmapPool());
    }

    public GifResourceDecoder(Context context, BitmapPool bitmapPool) {
        this.context = context;
        this.bitmapPool = bitmapPool;
    }

    @Override
    public GifResource decode(InputStream source, int width, int height) throws IOException {
        GifDecoder gifDecoder = new GifDecoder(bitmapPool);
        gifDecoder.read(source, 0);
        GifFrameManager frameManager = new GifFrameManager(context, bitmapPool);
        return new GifResource(gifDecoder, frameManager);
    }

    @Override
    public String getId() {
        return "GifResourceDecoder.com.bumptech.glide.load.gif";
    }
}
