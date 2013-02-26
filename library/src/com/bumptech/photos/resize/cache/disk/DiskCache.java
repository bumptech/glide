/*
 * Copyright (c) 2013. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.resize.cache.disk;

import android.graphics.Bitmap;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 1/26/13
 * Time: 9:20 AM
 * To change this template use File | Settings | File Templates.
 */
public class DiskCache {
    private static String JOURNAL_FILE_NAME = "JOURNAL";
    private static DiskCache CACHE = null;

    private final File outputDir;
    private Journal journal;
    private boolean isOpen = false;
    private Map<String, ReentrantLock> lockMap = new HashMap<String, ReentrantLock>();

    public static synchronized DiskCache get(File diskCacheDir, int maxCacheSize) {
        if (CACHE == null) {
            CACHE = new DiskCache(diskCacheDir, maxCacheSize);
        }

        return CACHE;
    }

    protected DiskCache(File outputDir, int maxCacheSize) {
        this.outputDir = outputDir;
        this.journal = new Journal(getFile(JOURNAL_FILE_NAME), maxCacheSize, new Journal.EvictionListener() {
            @Override
            public void onKeyEvicted(String safeKey) {
                delete(safeKey);
            }
        });
    }

    private void open() {
        if (isOpen) return;
        isOpen = true;
        try {
            journal.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void put(String key, final Bitmap bitmap) {
        synchronized (this) {
            if (!isOpen) open();
        }

        final String safeKey = sha1Hash(key);
        final Lock lock = acquireLockFor(safeKey);
        lock.lock();
        try {
            final File outFile = getFile(safeKey);

            OutputStream out = null;
            try {
                if (!outFile.exists()) outFile.createNewFile();

                out = new BufferedOutputStream(new FileOutputStream(outFile));
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
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
        ReentrantLock lock;
        synchronized (lockMap) {
            lock = lockMap.get(safeKey);
            if (lock == null) {
                lock = new ReentrantLock();
                lockMap.put(safeKey, lock);
            }
        }
        return lock;
    }

    public String get(String key) {
        synchronized (this) {
            if (!isOpen) open();
        }

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
        synchronized (this) {
            if (!isOpen) open();
        }
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
