/*
 * Copyright (c) 2013. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.resize.cache.disk;

import android.graphics.Bitmap;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created with IntelliJ IDEA.
 * User: sam
 * Date: 1/26/13
 * Time: 9:20 AM
 * To change this template use File | Settings | File Templates.
 */
public class DiskCache implements Closeable {
    private static String JOURNAL_FILE_NAME = "JOURNAL";
    private static DiskCache CACHE = null;

    private final File outputDir;
    private Journal journal;
    private boolean isOpen = false;

    public static DiskCache get(File diskCacheDir, int maxCacheSize) {
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

    public void open() {
        if (isOpen) return;
        isOpen = true;
        try {
            journal.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        if (!isOpen) return;
        isOpen = false;
        try {
            journal.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void put(String key, final Bitmap bitmap) {
        if (!isOpen) return;

        final String safeKey = sha1Hash(key);

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
    }

    public String get(String key) {
        if (!isOpen) return null;

        final String safeKey = sha1Hash(key);

        final File inFile = getFile(safeKey);
        if (!inFile.exists()) return null;

        try {
            journal.get(safeKey);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return inFile.getAbsolutePath();
    }

    public void remove(String key) {
        if (!isOpen) return;
        delete(sha1Hash(key));
    }

    private void delete(String safeKey) {
        final File toDelete = getFile(safeKey);
        toDelete.delete();
        try {
            journal.delete(safeKey);
        } catch (IOException e) {
            e.printStackTrace();
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
