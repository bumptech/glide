package com.bumptech.photos.resize.cache;

import android.graphics.Bitmap;
import com.bumptech.photos.util.Log;
import com.jakewharton.DiskLruCache;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A thin wrapper around Jake Wharton's disk cache library.
 *
 * @see com.jakewharton.DiskLruCache
 */
public class PhotoDiskCache {
    private final static int VALUE_COUNT = 1; //values per cache entry
    private DiskLruCache cache;
    private final File directory;
    private final long maxSize;
    private final int appVersion;

    public PhotoDiskCache(File directory, long maxSize, int appVersion) {
        this.directory = directory;
        this.maxSize = maxSize;
        this.appVersion = appVersion;
        start();
    }

    public void start() {
        if (cache != null && !cache.isClosed()) return;

        try {
            cache = DiskLruCache.open(directory, appVersion, VALUE_COUNT, maxSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if (cache == null) return;

        try {
            cache.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void put(final int key, final Bitmap bitmap) {
        if (cache.isClosed()) {
            Log.d("DLRU: put while cache is closed!");
            return;
        }

        if (bitmap == null) return;
        final String safeKey = sha1Hash(String.valueOf(key));

        DiskLruCache.Editor edit = null;
        OutputStream out = null;
        try {
            DiskLruCache.Snapshot snapshot = cache.get(safeKey);
            if (snapshot != null) {
                Log.d("DLRU: not putting, already exists key=" + key);
                return;
            }
            edit = cache.edit(safeKey);
            out = new BufferedOutputStream(edit.newOutputStream(VALUE_COUNT - 1));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            if (!cache.isClosed()) {
                edit.commit();
            }
        } catch (Exception e) {
            if (edit != null) {
                try {
                    edit.abort();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public InputStream get(final int key) {
        if (cache.isClosed()) {
            Log.d("DLRU: get while cache is closed key=" + key);
            return null;
        }
        //disk cache doesn't allow keys with anything but a-zA-Z0-9 :(
        final String safeKey = sha1Hash(String.valueOf(key));
        InputStream result = null;
        try {
            DiskLruCache.Snapshot snapshot = cache.get(safeKey);

            if (snapshot != null) {
                result = snapshot.getInputStream(VALUE_COUNT - 1);
            } else {
                Log.d("DLRU: snapshot not found key=" + key);
            }

        } catch (IOException e) {
            e.printStackTrace();
            try {
                cache.remove(safeKey);
            } catch (IOException e1) {
                Log.d("DLRU: error removing bitmap key=" + key);
                e1.printStackTrace();
            }
        }

        return result;
    }

    private static String sha1Hash(String toHash) {
        String hash = null;
        try {
            byte[] bytes = toHash.getBytes();
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(bytes, 0, bytes.length);
            hash = new BigInteger(1, digest.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hash;
    }
}
