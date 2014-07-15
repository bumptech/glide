package com.bumptech.glide.load.resource.gifbitmap;

import android.graphics.Bitmap;

import com.bumptech.glide.provider.DataLoadProvider;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.model.ImageVideoWrapper;
import com.bumptech.glide.load.resource.file.FileToStreamDecoder;
import com.bumptech.glide.load.resource.gif.GifData;

import java.io.File;
import java.io.InputStream;

/**
 * An {@link com.bumptech.glide.provider.DataLoadProvider} that can load either an
 * {@link com.bumptech.glide.load.resource.gif.GifDrawable} or an {@link Bitmap} from either an
 * {@link java.io.InputStream} or an {@link android.os.ParcelFileDescriptor}.
 */
public class ImageVideoGifDataLoadProvider implements DataLoadProvider<ImageVideoWrapper, GifBitmapWrapper> {
    private final ResourceDecoder<File, GifBitmapWrapper> cacheDecoder;
    private final ResourceDecoder<ImageVideoWrapper, GifBitmapWrapper> sourceDecoder;
    private final ResourceEncoder<GifBitmapWrapper> encoder;
    private final Encoder<ImageVideoWrapper> sourceEncoder;

    public ImageVideoGifDataLoadProvider(DataLoadProvider<ImageVideoWrapper, Bitmap> bitmapProvider,
            DataLoadProvider<InputStream, GifData> gifProvider) {
        cacheDecoder = new FileToStreamDecoder<GifBitmapWrapper>(new GifBitmapWrapperStreamResourceDecoder(new
                GifBitmapWrapperResourceDecoder(
                bitmapProvider.getSourceDecoder(),
                gifProvider.getSourceDecoder())));
        sourceDecoder = new GifBitmapWrapperResourceDecoder(
                bitmapProvider.getSourceDecoder(),
                gifProvider.getSourceDecoder());
        encoder = new GifBitmapWrapperResourceEncoder(bitmapProvider.getEncoder(), gifProvider.getEncoder());

        //TODO: what about the gif provider?
        sourceEncoder = bitmapProvider.getSourceEncoder();
    }

    @Override
    public ResourceDecoder<File, GifBitmapWrapper> getCacheDecoder() {
        return cacheDecoder;
    }

    @Override
    public ResourceDecoder<ImageVideoWrapper, GifBitmapWrapper> getSourceDecoder() {
        return sourceDecoder;
    }

    @Override
    public Encoder<ImageVideoWrapper> getSourceEncoder() {
        return sourceEncoder;
    }

    @Override
    public ResourceEncoder<GifBitmapWrapper> getEncoder() {
        return encoder;
    }
}
