package com.bumptech.glide.load.model;

import android.util.Log;

import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.util.ByteArrayPool;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An {@link com.bumptech.glide.load.Encoder} that can write an {@link java.io.InputStream} to disk.
 */
public class StreamEncoder implements Encoder<InputStream> {
    private static final String TAG = "StreamEncoder";

    @Override
    public boolean encode(InputStream data, OutputStream os) {
        byte[] buffer = ByteArrayPool.get().getBytes();
        try {
            int read;
            while ((read = data.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
            }
            return true;
        } catch (IOException e) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Failed to encode data onto the OutputStream", e);
            }
            return false;
        } finally {
            ByteArrayPool.get().releaseBytes(buffer);
        }
    }

    @Override
    public String getId() {
        return "";
    }
}
