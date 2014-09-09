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
 * An {@link ResourceDecoder} that can decode either an {@link Bitmap} or an {@link GifDrawable}
 * from an {@link InputStream} or a {@link android.os.ParcelFileDescriptor ParcelFileDescriptor}.
 */
public class GifBitmapWrapperResourceDecoder implements ResourceDecoder<ImageVideoWrapper, GifBitmapWrapper> {
    private static final ImageTypeParser DEFAULT_PARSER = new DefaultImageTypeParser();
    private static final BufferedStreamFactory DEFAULT_STREAM_FACTORY = new DefaultBufferedStreamFactory();
    // 2048 is rather arbitrary, for most well formatted image types we only need 32 bytes.
    // Visible for testing.
    static final int MARK_LIMIT_BYTES = 2048;

    private final ResourceDecoder<ImageVideoWrapper, Bitmap> bitmapDecoder;
    private final ResourceDecoder<InputStream, GifDrawable> gifDecoder;
    private final ImageTypeParser parser;
    private final BufferedStreamFactory streamFactory;
    private String id;

    public GifBitmapWrapperResourceDecoder(ResourceDecoder<ImageVideoWrapper, Bitmap> bitmapDecoder,
            ResourceDecoder<InputStream, GifDrawable> gifDecoder) {
        this(bitmapDecoder, gifDecoder, DEFAULT_PARSER, DEFAULT_STREAM_FACTORY);
    }

    // Visible for testing.
    GifBitmapWrapperResourceDecoder(ResourceDecoder<ImageVideoWrapper, Bitmap> bitmapDecoder,
            ResourceDecoder<InputStream, GifDrawable> gifDecoder, ImageTypeParser parser,
            BufferedStreamFactory streamFactory) {
        this.bitmapDecoder = bitmapDecoder;
        this.gifDecoder = gifDecoder;
        this.parser = parser;
        this.streamFactory = streamFactory;
    }

    @SuppressWarnings("resource")
    // @see ResourceDecoder.decode
    @Override
    public Resource<GifBitmapWrapper> decode(ImageVideoWrapper source, int width, int height) throws IOException {
        ByteArrayPool pool = ByteArrayPool.get();
        byte[] tempBytes = pool.getBytes();

        GifBitmapWrapper wrapper = null;
        try {
            wrapper = decode(source, width, height, tempBytes);
        } finally {
            pool.releaseBytes(tempBytes);
        }
        return wrapper != null ? new GifBitmapWrapperResource(wrapper) : null;
    }

    private GifBitmapWrapper decode(ImageVideoWrapper source, int width, int height, byte[] bytes) throws IOException {
        GifBitmapWrapper result = null;
        InputStream bis = null;
        if (source.getStream() != null) {
            bis = streamFactory.build(source.getStream(), bytes);
            bis.mark(MARK_LIMIT_BYTES);
            ImageHeaderParser.ImageType type = parser.parse(bis);
            bis.reset();

            if (type == ImageHeaderParser.ImageType.GIF) {
                Resource<GifDrawable> gifResource = gifDecoder.decode(bis, width, height);
                if (gifResource != null) {
                    result = new GifBitmapWrapper(null, gifResource);
                }
            }
        }

        if (result == null) {
            // We can only reset the buffered InputStream, so to start from the beginning of the stream, we need to pass
            // in a new source containing the buffered stream rather than the original stream.
            final ImageVideoWrapper wrapperToDecode;
            if (bis != null) {
                wrapperToDecode = new ImageVideoWrapper(bis, source.getFileDescriptor());
            } else {
                wrapperToDecode = source;
            }
            Resource<Bitmap> bitmapResource = bitmapDecoder.decode(wrapperToDecode, width, height);
            if (bitmapResource != null) {
                result = new GifBitmapWrapper(bitmapResource, null);
            }
        }

        return result;
    }

    @Override
    public String getId() {
        if (id == null) {
            id = gifDecoder.getId() + bitmapDecoder.getId();
        }
        return id;
    }

    interface BufferedStreamFactory {
        public InputStream build(InputStream is, byte[] buffer);
    }

    private static class DefaultBufferedStreamFactory implements BufferedStreamFactory {
        @Override
        public InputStream build(InputStream is, byte[] buffer) {
            return new RecyclableBufferedInputStream(is, buffer);
        }
    }

    interface ImageTypeParser {
        public ImageHeaderParser.ImageType parse(InputStream is) throws IOException;
    }

    private static class DefaultImageTypeParser implements ImageTypeParser {
        @Override
        public ImageHeaderParser.ImageType parse(InputStream is) throws IOException {
            return new ImageHeaderParser(is).getType();
        }
    }
}
