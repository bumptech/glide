package com.bumptech.glide.load.resource.gif;

import android.content.Context;
import android.util.Log;
import com.bumptech.glide.Glide;
import com.bumptech.glide.gifdecoder.GifHeader;
import com.bumptech.glide.gifdecoder.GifHeaderParser;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.util.Util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class GifResourceDecoder implements ResourceDecoder<InputStream, GifData> {
    private static final String TAG = "GifResourceDecoder";
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
    public GifDataResource decode(InputStream source, int width, int height) throws IOException {
        byte[] data = inputStreamToBytes(source);
        GifHeader header = new GifHeaderParser(data).parseHeader();
        String id = getGifId(data);
        return new GifDataResource(new GifData(context, bitmapPool, id, header, data, width, height));
    }

    @Override
    public String getId() {
        return "GifResourceDecoder.com.bumptech.glide.load.resource.gif";
    }

    private String getGifId(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(data);
            return Util.sha256BytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            if (Log.isLoggable(TAG, Log.WARN)) {
                Log.w(TAG, "Missing sha1 algorithm?", e);
            }
        }
        return UUID.randomUUID().toString();
    }

    private byte[] inputStreamToBytes(InputStream is) {
        int capacity = 16384;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(capacity);
        try {
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
        } catch (IOException e) {
            Log.w(TAG, "Error reading data from stream", e);
        }
        return buffer.toByteArray();
    }
}
