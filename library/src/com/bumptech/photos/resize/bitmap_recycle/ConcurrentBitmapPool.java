/*
 * Copyright (c) 2012. Bump Technologies Inc. All Rights Reserved.
 */

package com.bumptech.photos.resize.bitmap_recycle;

import android.graphics.Bitmap;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A cache of Bitmaps made available by size used to manage recycled bitmaps
 */
public class ConcurrentBitmapPool implements BitmapPool {
    private static final int DEFAULT_MAX_PER_SIZE = 20;
    private ConcurrentHashMap<Integer, Queue<Bitmap>> availableBitmaps = new ConcurrentHashMap<Integer, Queue<Bitmap>>();
    private final int maxPerSize;

    public ConcurrentBitmapPool(int maxPerSize) {
        this.maxPerSize = maxPerSize == 0 ? DEFAULT_MAX_PER_SIZE : maxPerSize;
    }

    @Override
    public boolean put(Bitmap bitmap) {
        final int sizeKey = getSizeKey(bitmap.getWidth(), bitmap.getHeight());
        Queue<Bitmap> available = availableBitmaps.get(sizeKey);
        if (available == null) {
            synchronized (this) {
                available = availableBitmaps.get(sizeKey);
                if (available == null) {
                    available = new ArrayDeque<Bitmap>(maxPerSize);
                    availableBitmaps.put(sizeKey, available);
                }
            }
        }

        final boolean result;
        synchronized (available) {
            result = available.size() < maxPerSize;
            if (result) {
                available.offer(bitmap);
            }
        }

        return result;
    }

    @Override
    public Bitmap get(int width, int height) {
        final int sizeKey = getSizeKey(width, height);
        final Queue<Bitmap> available = availableBitmaps.get(sizeKey);
        Bitmap result = null;
        if (available != null) {
            synchronized (available) {
                result = available.poll();
            }
        }
        return result;
    }

    //see http://szudzik.com/ElegantPairing.pdf
    //assumes width <= Short.MAX_VALUE && height <= SHORT.MAX_VALUE && width >= 0 && height >= 0
    private static int getSizeKey(int width, int height) {
        return width >= height ? width * width + width + height : width + height * height;
    }
}
