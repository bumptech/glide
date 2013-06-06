/*
 * Copyright (c) 2013. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.resize.cache.disk;

import android.graphics.Bitmap;
import com.bumptech.photos.resize.cache.DiskCache;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 1/26/13
 * Time: 9:20 AM
 * To change this template use File | Settings | File Templates.
 */
public class AndroidDiskCache implements DiskCache {
    private static final String JOURNAL_FILE_NAME = "JOURNAL";
    private static AndroidDiskCache CACHE = null;

    private final File outputDir;
    private Journal journal;
    private ConcurrentHashMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<String, ReentrantLock>();

    public static synchronized AndroidDiskCache get(File diskCacheDir, int maxCacheSize) {
        if (CACHE == null) {
            CACHE = new AndroidDiskCache(diskCacheDir, maxCacheSize);
            CACHE.open();
        }

        return CACHE;
    }

    protected AndroidDiskCache(File outputDir, int maxCacheSize) {
        this.outputDir = outputDir;
        this.journal = new Journal(getFile(JOURNAL_FILE_NAME), maxCacheSize, new Journal.EvictionListener() {
            @Override
            public void onKeyEvicted(String safeKey) {
                delete(safeKey);
            }
        });
    }

    private void open() {
        try {
            journal.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void put(String key, final Bitmap bitmap, Bitmap.CompressFormat format) {
        final String safeKey = sha1Hash(key);
        final Lock lock = acquireLockFor(safeKey);
        lock.lock();
        try {
            final File outFile = getFile(safeKey);

            OutputStream out = null;
            try {
                if (!outFile.exists()) outFile.createNewFile();

                out = new BufferedOutputStream(new FileOutputStream(outFile), 8192);
                bitmap.compress(format, 100, out);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e1) { }
                }
            }
            try {
                journal.put(safeKey, (int) outFile.length());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            lock.unlock();
        }
    }

    private Lock acquireLockFor(String safeKey) {
        ReentrantLock lock = lockMap.get(safeKey);
        if (lock == null) {
            ReentrantLock newLock = new ReentrantLock();
            lock = lockMap.putIfAbsent(safeKey, newLock);
            if (lock == null) {
                lock = newLock;
            }
        }
        return lock;
    }

    @Override
    public String get(String key) {

        final String safeKey = sha1Hash(key);
        Lock lock = acquireLockFor(safeKey);
        lock.lock();
        try {
            final File inFile = getFile(safeKey);
            if (!inFile.exists()) return null;

            try {
                journal.get(safeKey);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return inFile.getAbsolutePath();
        } finally {
            lock.unlock();
        }
    }

    public void remove(String key) {
        delete(sha1Hash(key));
    }

    private void delete(String safeKey) {
        final Lock lock = acquireLockFor(safeKey);
        lock.lock();
        try {
            final File toDelete = getFile(safeKey);
            toDelete.delete();
            try {
                journal.delete(safeKey);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            lock.unlock();
        }
    }

    private File getFile(String safeKey) {
        return new File(outputDir + File.separator + safeKey);
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
