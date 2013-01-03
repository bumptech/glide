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
 * Created by IntelliJ IDEA.
 * User: sam
 * Date: 10/20/12
 * Time: 1:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class PhotoDiskCache {
    private final static int VALUE_COUNT = 1; //values per cache entry
    private DiskLruCache cache;

    public PhotoDiskCache(File directory, long maxSize, int appVersion) {
        try {
            cache = DiskLruCache.open(directory, appVersion, VALUE_COUNT, maxSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void put(final String key, final Bitmap bitmap) {
        if (bitmap == null) return;
        final String safeKey = sha1Hash(key);

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
            edit.commit();
        } catch (IOException e) {
            if (edit != null) {
                try {
                    edit.abort();
                } catch (IOException e1) {
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

    public InputStream get(final String key) {
        //disk cache doesn't allow keys with anything but a-zA-Z0-9 :(
        final String safeKey = sha1Hash(key);
        InputStream result = null;
        try {
            DiskLruCache.Snapshot snapshot = cache.get(safeKey);

            if (snapshot != null) {
                result = snapshot.getInputStream(VALUE_COUNT - 1);
            } else {
                Log.d("DLRU: snapshot not found key=" + key);
            }

        } catch (IOException e) {
            Log.d("DLRU: IOException? key=" + key);
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            try {
                cache.remove(safeKey);
            } catch (IOException e1) {
                Log.d("DLRU: error removing bitmap key=" + key);
                e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
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
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return hash;
    }
}
