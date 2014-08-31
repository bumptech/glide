package com.bumptech.glide.load.resource.gif;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.gifdecoder.GifHeader;
import com.bumptech.glide.gifdecoder.GifHeaderParser;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.UnitTransformation;
import com.bumptech.glide.util.Util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.UUID;

/**
 * An {@link com.bumptech.glide.load.ResourceDecoder} that decodes
 * {@link com.bumptech.glide.load.resource.gif.GifDrawable} from {@link java.io.InputStream} data.
 */
public class GifResourceDecoder implements ResourceDecoder<InputStream, GifDrawable> {
    private static final String TAG = "GifResourceDecoder";
    private static final GifHeaderParserPool PARSER_POOL = new DefaultGifHeaderParserPool();
    private final Context context;
    private final BitmapPool bitmapPool;
    private final GifHeaderParserPool parserPool;

    public GifResourceDecoder(Context context) {
        this(context, Glide.get(context).getBitmapPool());
    }

    public GifResourceDecoder(Context context, BitmapPool bitmapPool) {
        this(context, bitmapPool, PARSER_POOL);
    }

    GifResourceDecoder(Context context, BitmapPool bitmapPool, GifHeaderParserPool parserPool) {
        this.context = context;
        this.bitmapPool = bitmapPool;
        this.parserPool = parserPool;
    }

    @Override
    public GifDrawableResource decode(InputStream source, int width, int height) {
        byte[] data = inputStreamToBytes(source);
        final GifHeaderParser parser = parserPool.obtain(data);
        try {
            return decode(data, width, height, parser);
        } finally {
            parserPool.release(parser);
        }
    }

    private GifDrawableResource decode(byte[] data, int width, int height, GifHeaderParser parser) {
        final GifHeader header = parser.parseHeader();
        if (header.getNumFrames() <= 0) {
            // If we couldn't decode the GIF, we will end up with a frame count of 0.
            return null;
        }

        String id = getGifId(data);

        Transformation<Bitmap> transformation = UnitTransformation.get();
        GifDrawable gifDrawable = new GifDrawable(context, new GifBitmapProvider(bitmapPool), transformation, width,
                height, id, header, data, header.getWidth(), header.getHeight());

        return new GifDrawableResource(gifDrawable);
    }

    @Override
    public String getId() {
        return "";
    }

    // A best effort attempt to get a unique id that can be used as a cache key for frames of the decoded GIF.
    private static String getGifId(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(data);
            return Util.sha1BytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            if (Log.isLoggable(TAG, Log.WARN)) {
                Log.w(TAG, "Missing sha1 algorithm?", e);
            }
        }
        return UUID.randomUUID().toString();
    }

    private static byte[] inputStreamToBytes(InputStream is) {
        final int bufferSize = 16384, initialCapacity = bufferSize;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(initialCapacity);
        try {
            int nRead;
            byte[] data = new byte[bufferSize];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
        } catch (IOException e) {
            Log.w(TAG, "Error reading data from stream", e);
        }
        //TODO the returned byte[] may be partial if an IOException was thrown from read
        return buffer.toByteArray();
    }

    interface GifHeaderParserPool {
        public GifHeaderParser obtain(byte[] data);
        public void release(GifHeaderParser parser);
    }

    private static class DefaultGifHeaderParserPool implements GifHeaderParserPool {
        private static final Queue<GifHeaderParser> POOL = new ArrayDeque<GifHeaderParser>();

        @Override
        public GifHeaderParser obtain(byte[] data) {
            GifHeaderParser result;
            synchronized (POOL) {
                result = POOL.poll();
            }
            if (result == null) {
                result = new GifHeaderParser();
            }

            return result.setData(data);
        }

        @Override
        public void release(GifHeaderParser parser) {
            synchronized (POOL) {
                POOL.offer(parser);
            }
        }
    }

    private static class GifBitmapProvider implements GifDecoder.BitmapProvider {
        private BitmapPool bitmapPool;

        public GifBitmapProvider(BitmapPool bitmapPool) {
            this.bitmapPool = bitmapPool;
        }

        @Override
        public Bitmap obtain(int width, int height, Bitmap.Config config) {
            return bitmapPool.get(width, height, config);
        }
    }
}
