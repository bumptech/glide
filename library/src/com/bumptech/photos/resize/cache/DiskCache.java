package com.bumptech.photos.resize.cache;

import android.graphics.Bitmap;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 6/5/13
 * Time: 10:21 AM
 * To change this template use File | Settings | File Templates.
 */
public interface DiskCache {
    public interface Writer {
        public void write(OutputStream os);
    }

    public interface Reader {
        public Bitmap read(InputStream is1, InputStream is2);
    }

    public Bitmap get(String key, Reader reader);
    public void put(String key, Writer writer);
    public void delete(String key);
}
