package com.bumptech.glide.load.resource.gifbitmap;

import android.graphics.Bitmap;

import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.model.ImageVideoWrapper;
import com.bumptech.glide.load.resource.bitmap.ImageHeaderParser;
import com.bumptech.glide.load.resource.bitmap.RecyclableBufferedInputStream;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.util.ByteArrayPool;

import java.io.IOException;
import java.io.InputStream;

/**
 * An {@link com.bumptech.glide.load.ResourceDecoder} that can decode either an {@link android.graphics.Bitmap} or an
 * {@link com.bumptech.glide.load.resource.gif.GifDrawable} from an {@link java.io.InputStream} or an
 * {@link android.os.ParcelFileDescriptor}.
 */
public class GifBitmapWrapperResourceDecoder implements ResourceDecoder<ImageVideoWrapper, GifBitmapWrapper> {
    private final ResourceDecoder<ImageVideoWrapper, Bitmap> bitmapDecoder;
    private final ResourceDecoder<InputStream, GifDrawable> gifDecoder;
    private String id;

    public GifBitmapWrapperResourceDecoder(ResourceDecoder<ImageVideoWrapper, Bitmap> bitmapDecoder,
            ResourceDecoder<InputStream, GifDrawable> gifDecoder) {
        this.bitmapDecoder = bitmapDecoder;
        this.gifDecoder = gifDecoder;
    }

    @SuppressWarnings("resource")
    // @see ResourceDecoder.decode
    @Override
    public Resource<GifBitmapWrapper> decode(ImageVideoWrapper source, int width, int height) throws IOException {
        ByteArrayPool pool = ByteArrayPool.get();
        InputStream is = source.getStream();
        byte[] tempBytes = pool.getBytes();
        RecyclableBufferedInputStream bis = new RecyclableBufferedInputStream(is, tempBytes);
        try {
            GifBitmapWrapper result = null;
            if (is != null) {
                source = new ImageVideoWrapper(bis, source.getFileDescriptor());
                // 2048 is rather arbitrary, for most well formatted image types we only need 32 bytes.
                bis.mark(2048);
                ImageHeaderParser.ImageType type = new ImageHeaderParser(bis).getType();
                bis.reset();

                if (type == ImageHeaderParser.ImageType.GIF) {
                    Resource<GifDrawable> gifResource = gifDecoder.decode(bis, width, height);
                    result = new GifBitmapWrapper(null, gifResource);
                }
            }

            if (result == null) {
                Resource<Bitmap> bitmapResource = bitmapDecoder.decode(source, width, height);
                result = new GifBitmapWrapper(bitmapResource, null);
            }
            return new GifBitmapWrapperResource(result);
        } finally {
            pool.releaseBytes(tempBytes);
        }
    }

    @Override
    public String getId() {
        if (id == null) {
            id = gifDecoder.getId() + bitmapDecoder.getId();
        }
        return id;
    }
}
