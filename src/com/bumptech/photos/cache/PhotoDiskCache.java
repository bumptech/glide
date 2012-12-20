package com.bumptech.photos.cache;

import android.graphics.Bitmap;
import android.os.Handler;
import com.bumptech.photos.util.Log;
import com.jakewharton.DiskLruCache;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: sam
 * Date: 10/20/12
 * Time: 1:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class PhotoDiskCache {
    private static int APP_VERSION = 0;
    private static int VALUE_COUNT = 1; //values per cache entry
    private DiskLruCache cache;
    private Handler mainHandler;
    private Handler getHandler;
    private Handler putHandler;

    public interface GetCallback {
        public void onGet(InputStream is);
    };

    public PhotoDiskCache(File directory, long maxSize, Handler mainHandler, Handler loadHandler) {
        try {
            cache = DiskLruCache.open(directory, APP_VERSION, VALUE_COUNT, maxSize);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        this.putHandler = loadHandler;
        this.mainHandler = mainHandler;
        this.getHandler = loadHandler;
    }

    public void put(final String key, final Bitmap bitmap) {
        if (bitmap == null) return;

        Log.d("DLRU: doPut key=" + key);
        putHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d("DLRU: run put key=" + key);
                DiskLruCache.Editor edit = null;
                OutputStream out = null;
                try {
                    DiskLruCache.Snapshot snapshot = cache.get(key);
                    if (snapshot != null) {
                        Log.d("DLRU: not putting, already exists key=" + key);
                        return;
                    }
                    edit = cache.edit(key);
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
        });
    }

    public void get(final String key, final GetCallback cb) {
        Log.d("DLRU: get key=" + key);
        getHandler.post(new Runnable() {
            @Override
            public void run() {
                InputStream result = null;
                try {
                    DiskLruCache.Snapshot snapshot = cache.get(key);

                    if (snapshot != null) {
                        result = snapshot.getInputStream(VALUE_COUNT - 1);
                    } else {
                        Log.d("DLRU: not found key=" + key);
                    }
                } catch (IOException e) {
                    Log.d("DLRU: IOException? key=" + key);
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    try {
                        cache.remove(key);
                    } catch (IOException e1) {
                        Log.d("DLRU: error removing bitmap key=" + key);
                        e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }

                final InputStream finalResult = result;
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        cb.onGet(finalResult);
                    }
                });
            }
        });
    }
}
