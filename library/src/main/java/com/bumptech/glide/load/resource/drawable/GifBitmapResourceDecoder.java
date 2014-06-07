package com.bumptech.glide.load.resource.drawable;

import android.content.Context;
import android.graphics.Bitmap;
import com.bumptech.glide.Resource;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.model.ImageVideoWrapper;
import com.bumptech.glide.load.resource.bitmap.ImageHeaderParser;
import com.bumptech.glide.load.resource.bitmap.RecyclableBufferedInputStream;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.util.ByteArrayPool;

import java.io.IOException;
import java.io.InputStream;

public class GifBitmapResourceDecoder implements ResourceDecoder<ImageVideoWrapper, GifBitmap> {
    private Context context;
    private final ResourceDecoder<ImageVideoWrapper, Bitmap> bitmapDecoder;
    private final ResourceDecoder<InputStream, GifDrawable> gifDecoder;

    public GifBitmapResourceDecoder(Context context, ResourceDecoder<ImageVideoWrapper, Bitmap> bitmapDecoder,
            ResourceDecoder<InputStream, GifDrawable> gifDecoder) {
        this.context = context;
        this.bitmapDecoder = bitmapDecoder;
        this.gifDecoder = gifDecoder;
    }

    @Override
    public Resource<GifBitmap> decode(ImageVideoWrapper source, int width, int height) throws IOException {
        ByteArrayPool pool = ByteArrayPool.get();
        InputStream is = source.getStream();
        GifBitmap result = null;
        if (is != null) {
            byte[] tempBytes = pool.getBytes();
            RecyclableBufferedInputStream bis = new RecyclableBufferedInputStream(is, tempBytes);
            bis.mark(1024);
            ImageHeaderParser.ImageType type = new ImageHeaderParser(bis).getType();
            bis.reset();

            if (type == ImageHeaderParser.ImageType.GIF) {
                Resource<GifDrawable> gifResource = gifDecoder.decode(is, width, height);
                result = new GifBitmap(gifResource);
            }
            pool.releaseBytes(tempBytes);
        }

        if (result == null) {
            Resource<Bitmap> bitmapResource = bitmapDecoder.decode(source, width, height);
            result = new GifBitmap(context.getResources(), bitmapResource);
        }
        return new GifBitmapResource(result);
    }

    @Override
    public String getId() {
        return "GifBitmapResourceDecoder.com.bumptech.glide.load.resource.drawable";
    }
}
