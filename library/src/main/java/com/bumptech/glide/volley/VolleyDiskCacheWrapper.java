package com.bumptech.glide.volley;

import android.util.Log;
import com.android.volley.Cache;
import com.android.volley.toolbox.ByteArrayPool;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.PoolingByteArrayOutputStream;
import com.bumptech.glide.load.engine.cache.StringKey;
import com.bumptech.glide.load.engine.cache.DiskCache;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Closely based on {@link DiskBasedCache}.
 */
public class VolleyDiskCacheWrapper implements Cache {
    private static final String TAG = "VolleyDiskCacheWrapper";
    /** Magic number for current version of cache file format. */
    private static final int CACHE_MAGIC = 0x20120504;
    // 2 mb.
    private static final int BYTE_POOL_SIZE = 2 * 1024 * 1024;
    // 8 kb.
    private static final int DEFAULT_BYTE_ARRAY_SIZE = 8 * 1024;

    private final DiskCache diskCache;
    private final ByteArrayPool byteArrayPool;

    public VolleyDiskCacheWrapper(DiskCache diskCache) {
        this.diskCache = diskCache;
        this.byteArrayPool = new ByteArrayPool(BYTE_POOL_SIZE);
    }

    @Override
    public Entry get(String key) {
        InputStream result = diskCache.get(new StringKey(key));
        if (result == null) {
            return null;
        }
        try {
            CacheHeader header = readHeader(result);
            byte[] data = streamToBytes(result);
            return header.toCacheEntry(data);
        } catch (IOException e) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                e.printStackTrace();
            }
            diskCache.delete(new StringKey(key));
        } finally {
            try {
                result.close();
            } catch (IOException e) { }
        }
        return null;
    }

    @Override
    public void put(final String key, final Entry entry) {
        diskCache.put(new StringKey(key), new DiskCache.Writer() {
            @Override
            public boolean write(OutputStream os) {
                CacheHeader header = new CacheHeader(key, entry);
                boolean success = header.writeHeader(os);
                if (success) {
                    try {
                        os.write(entry.data);
                    } catch (IOException e) {
                        success = false;
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "Unable to write data", e);
                        }
                    }
                }
                return success;
            }
        });
    }

    @Override
    public void initialize() { }

    @Override
    public void invalidate(String key, boolean fullExpire) {
        Entry entry = get(key);
        if (entry != null) {
            entry.softTtl = 0;
            if (fullExpire) {
                entry.ttl = 0;
            }
            put(key, entry);
        }
    }

    @Override
    public void remove(String key) {
        diskCache.delete(new StringKey(key));
    }

    @Override
    public void clear() { }

    /**
     * Reads the header off of an InputStream and returns a CacheHeader object.
     * @param is The InputStream to read from.
     * @throws IOException
     */
    public CacheHeader readHeader(InputStream is) throws IOException {
        CacheHeader entry = new CacheHeader();
        int magic = readInt(is);
        if (magic != CACHE_MAGIC) {
            // don't bother deleting, it'll get pruned eventually
            throw new IOException();
        }
        entry.key = readString(is);
        entry.etag = readString(is);
        if (entry.etag.equals("")) {
            entry.etag = null;
        }
        entry.serverDate = readLong(is);
        entry.ttl = readLong(is);
        entry.softTtl = readLong(is);
        entry.responseHeaders = readStringStringMap(is);
        return entry;
    }

        /**
     * Handles holding onto the cache headers for an entry.
     */
    // Visible for testing.
    class CacheHeader {
        /** The size of the data identified by this CacheHeader. (This is not
         * serialized to disk. */
        public long size;

        /** The key that identifies the cache entry. */
        public String key;

        /** ETag for cache coherence. */
        public String etag;

        /** Date of this response as reported by the server. */
        public long serverDate;

        /** TTL for this record. */
        public long ttl;

        /** Soft TTL for this record. */
        public long softTtl;

        /** Headers from the response resulting in this cache entry. */
        public Map<String, String> responseHeaders;

        private CacheHeader() { }

        /**
         * Instantiates a new CacheHeader object
         * @param key The key that identifies the cache entry
         * @param entry The cache entry.
         */
        public CacheHeader(String key, Entry entry) {
            this.key = key;
            this.size = entry.data.length;
            this.etag = entry.etag;
            this.serverDate = entry.serverDate;
            this.ttl = entry.ttl;
            this.softTtl = entry.softTtl;
            this.responseHeaders = entry.responseHeaders;
        }

        /**
         * Creates a cache entry for the specified data.
         */
        public Entry toCacheEntry(byte[] data) {
            Entry e = new Entry();
            e.data = data;
            e.etag = etag;
            e.serverDate = serverDate;
            e.ttl = ttl;
            e.softTtl = softTtl;
            e.responseHeaders = responseHeaders;
            return e;
        }

        /**
         * Writes the contents of this CacheHeader to the specified OutputStream.
         */
        public boolean writeHeader(OutputStream os) {
            try {
                writeInt(os, CACHE_MAGIC);
                writeString(os, key);
                writeString(os, etag == null ? "" : etag);
                writeLong(os, serverDate);
                writeLong(os, ttl);
                writeLong(os, softTtl);
                writeStringStringMap(responseHeaders, os);
                os.flush();
                return true;
            } catch (IOException e) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d("%s", e.toString());
                }
                return false;
            }
        }
    }

    /*
     * Homebrewed simple serialization system used for reading and writing cache
     * headers on disk. Once upon a time, this used the standard Java
     * Object{Input,Output}Stream, but the default implementation relies heavily
     * on reflection (even for standard types) and generates a ton of garbage.
     */

    /**
     * Simple wrapper around {@link InputStream#read()} that throws EOFException
     * instead of returning -1.
     */
    private static int read(InputStream is) throws IOException {
        int b = is.read();
        if (b == -1) {
            throw new EOFException();
        }
        return b;
    }

    static void writeInt(OutputStream os, int n) throws IOException {
        os.write((n >> 0) & 0xff);
        os.write((n >> 8) & 0xff);
        os.write((n >> 16) & 0xff);
        os.write((n >> 24) & 0xff);
    }

    static int readInt(InputStream is) throws IOException {
        int n = 0;
        n |= (read(is) << 0);
        n |= (read(is) << 8);
        n |= (read(is) << 16);
        n |= (read(is) << 24);
        return n;
    }

    static void writeLong(OutputStream os, long n) throws IOException {
        os.write((byte)(n >>> 0));
        os.write((byte)(n >>> 8));
        os.write((byte)(n >>> 16));
        os.write((byte)(n >>> 24));
        os.write((byte)(n >>> 32));
        os.write((byte)(n >>> 40));
        os.write((byte)(n >>> 48));
        os.write((byte)(n >>> 56));
    }

    static long readLong(InputStream is) throws IOException {
        long n = 0;
        n |= ((read(is) & 0xFFL) << 0);
        n |= ((read(is) & 0xFFL) << 8);
        n |= ((read(is) & 0xFFL) << 16);
        n |= ((read(is) & 0xFFL) << 24);
        n |= ((read(is) & 0xFFL) << 32);
        n |= ((read(is) & 0xFFL) << 40);
        n |= ((read(is) & 0xFFL) << 48);
        n |= ((read(is) & 0xFFL) << 56);
        return n;
    }

    static void writeString(OutputStream os, String s) throws IOException {
        byte[] b = s.getBytes("UTF-8");
        writeLong(os, b.length);
        os.write(b, 0, b.length);
    }

    String readString(InputStream is) throws IOException {
        int n = (int) readLong(is);
        byte[] b = streamToBytes(is, n, byteArrayPool.getBuf(n));
        String result = new String(b, "UTF-8");
        byteArrayPool.returnBuf(b);
        return result;
    }

    static void writeStringStringMap(Map<String, String> map, OutputStream os) throws IOException {
        if (map != null) {
            writeInt(os, map.size());
            for (Map.Entry<String, String> entry : map.entrySet()) {
                writeString(os, entry.getKey());
                writeString(os, entry.getValue());
            }
        } else {
            writeInt(os, 0);
        }
    }

    Map<String, String> readStringStringMap(InputStream is) throws IOException {
        int size = readInt(is);
        Map<String, String> result = (size == 0)
                ? Collections.<String, String>emptyMap()
                : new HashMap<String, String>(size);
        for (int i = 0; i < size; i++) {
            String key = readString(is).intern();
            String value = readString(is).intern();
            result.put(key, value);
        }
        return result;
    }

    /**
     * Reads the contents of an InputStream into a byte[].
     */
    private static byte[] streamToBytes(InputStream in, int length, byte[] bytes) throws IOException {
        int count;
        int pos = 0;
        while (pos < length && ((count = in.read(bytes, pos, length - pos)) != -1)) {
            pos += count;
        }
        if (pos != length) {
            throw new IOException("Expected " + length + " bytes, read " + pos + " bytes");
        }
        return bytes;
    }

    private byte[] streamToBytes(InputStream in) throws IOException {
        PoolingByteArrayOutputStream outputStream = new PoolingByteArrayOutputStream(byteArrayPool);
        byte[] bytes = byteArrayPool.getBuf(DEFAULT_BYTE_ARRAY_SIZE);
        int pos = 0;
        while ((in.read(bytes, pos, bytes.length - pos)) != -1) {
            outputStream.write(bytes);
        }
        byteArrayPool.returnBuf(bytes);
        byte[] result = outputStream.toByteArray();
        outputStream.close();
        return result;
    }
}
